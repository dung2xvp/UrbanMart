package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.FoodIncompatibilityWriteRequest;
import com.haui.UrbanMart.dto.request.FoodTagCreateRequest;
import com.haui.UrbanMart.dto.request.IncompatibilityCheckRequest;
import com.haui.UrbanMart.dto.request.ProductFoodTagsRequest;
import com.haui.UrbanMart.dto.response.FoodIncompatibilityResponse;
import com.haui.UrbanMart.dto.response.FoodTagResponse;
import com.haui.UrbanMart.dto.response.IncompatibilityCheckResponse;
import com.haui.UrbanMart.dto.response.ProductFoodTagsResponse;
import com.haui.UrbanMart.dto.response.ProductIncompatibilityResponse;
import com.haui.UrbanMart.entity.FoodIncompatibility;
import com.haui.UrbanMart.entity.FoodTag;
import com.haui.UrbanMart.entity.IncompatibilitySeverity;
import com.haui.UrbanMart.entity.Product;
import com.haui.UrbanMart.entity.ProductStatus;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.exception.ConflictException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.FoodIncompatibilityRepository;
import com.haui.UrbanMart.repository.FoodTagRepository;
import com.haui.UrbanMart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodIncompatibilityService {

    private final FoodIncompatibilityRepository ruleRepository;
    private final FoodTagRepository tagRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<FoodIncompatibilityResponse> getRules(
            String search,
            IncompatibilitySeverity severity,
            Boolean active,
            Pageable pageable
    ) {
        String normalized = search == null || search.isBlank() ? null : search.trim();
        return ruleRepository.search(normalized, severity, active, pageable)
                .map(FoodIncompatibilityResponse::from);
    }

    @Transactional(readOnly = true)
    public FoodIncompatibilityResponse getRule(UUID id) {
        return FoodIncompatibilityResponse.from(findRule(id));
    }

    @Transactional
    public FoodIncompatibilityResponse createRule(FoodIncompatibilityWriteRequest request) {
        String[] pair = normalizePair(request.tagA(), request.tagB());
        requireTags(List.of(pair));
        if (ruleRepository.findByTagAAndTagB(pair[0], pair[1]).isPresent()) {
            throw new ConflictException("Quy tắc cho cặp tag này đã tồn tại");
        }
        FoodIncompatibility rule = new FoodIncompatibility();
        applyRule(rule, request, pair);
        return FoodIncompatibilityResponse.from(ruleRepository.save(rule));
    }

    @Transactional
    public FoodIncompatibilityResponse updateRule(
            UUID id,
            FoodIncompatibilityWriteRequest request
    ) {
        FoodIncompatibility rule = findRule(id);
        String[] pair = normalizePair(request.tagA(), request.tagB());
        requireTags(List.of(pair));
        ruleRepository.findByTagAAndTagB(pair[0], pair[1])
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Quy tắc cho cặp tag này đã tồn tại");
                });
        applyRule(rule, request, pair);
        return FoodIncompatibilityResponse.from(rule);
    }

    @Transactional
    public UUID deleteRule(UUID id) {
        FoodIncompatibility rule = findRule(id);
        ruleRepository.delete(rule);
        return id;
    }

    @Transactional(readOnly = true)
    public List<FoodTagResponse> getTags() {
        return tagRepository.findAllByOrderByCodeAsc().stream()
                .map(FoodTagResponse::from)
                .toList();
    }

    @Transactional
    public FoodTagResponse createTag(FoodTagCreateRequest request) {
        String code = normalizeTag(request.code());
        if (tagRepository.existsById(code)) {
            throw new ConflictException("Mã tag đã tồn tại");
        }
        FoodTag tag = new FoodTag();
        tag.setCode(code);
        tag.setName(request.name().trim());
        return FoodTagResponse.from(tagRepository.save(tag));
    }

    @Transactional
    public ProductFoodTagsResponse setProductTags(
            UUID productId,
            ProductFoodTagsRequest request
    ) {
        Product product = productRepository.findWithFoodTagsById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        List<String> codes = request.tagCodes().stream()
                .map(FoodIncompatibilityService::normalizeTag)
                .toList();
        if (new HashSet<>(codes).size() != codes.size()) {
            throw new BadRequestException("Danh sách tag bị trùng");
        }
        List<FoodTag> tags = codes.isEmpty()
                ? List.of()
                : tagRepository.findAllById(codes);
        if (tags.size() != codes.size()) {
            Set<String> found = tags.stream().map(FoodTag::getCode).collect(Collectors.toSet());
            String missing = codes.stream().filter(code -> !found.contains(code))
                    .findFirst().orElseThrow();
            throw new ResourceNotFoundException("Không tìm thấy tag " + missing);
        }
        product.getFoodTags().clear();
        product.getFoodTags().addAll(tags);
        return toProductTags(product);
    }

    @Transactional(readOnly = true)
    public IncompatibilityCheckResponse checkProducts(IncompatibilityCheckRequest request) {
        List<UUID> ids = request.productIds().stream().distinct().toList();
        List<Product> products = productRepository.findWithFoodTagsByIdIn(ids);
        Map<UUID, Product> byId = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (UUID id : ids) {
            Product product = byId.get(id);
            if (product == null) {
                throw new ResourceNotFoundException("Không tìm thấy sản phẩm " + id);
            }
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new BadRequestException("Sản phẩm " + product.getName()
                        + " đã ngừng kinh doanh");
            }
        }
        Set<String> codes = products.stream()
                .flatMap(product -> product.getFoodTags().stream())
                .map(FoodTag::getCode)
                .collect(Collectors.toSet());
        if (codes.size() < 2) {
            return new IncompatibilityCheckResponse(false, List.of());
        }

        Map<String, List<Product>> productsByTag = new LinkedHashMap<>();
        products.forEach(product -> product.getFoodTags().forEach(tag ->
                productsByTag.computeIfAbsent(tag.getCode(), key -> new ArrayList<>())
                        .add(product)
        ));
        List<IncompatibilityCheckResponse.Warning> warnings = new ArrayList<>();
        for (FoodIncompatibility rule : ruleRepository.findActiveWithinTags(codes)) {
            List<IncompatibilityCheckResponse.ConflictingProduct> matches = new ArrayList<>();
            addConflictingProducts(matches, productsByTag.get(rule.getTagA()), rule.getTagA());
            addConflictingProducts(matches, productsByTag.get(rule.getTagB()), rule.getTagB());
            warnings.add(new IncompatibilityCheckResponse.Warning(
                    rule.getId(),
                    rule.getSeverity(),
                    rule.getTitle(),
                    rule.getEffect(),
                    rule.getRecommendation(),
                    matches
            ));
        }
        return new IncompatibilityCheckResponse(!warnings.isEmpty(), warnings);
    }

    @Transactional(readOnly = true)
    public ProductIncompatibilityResponse getProductRules(UUID productId) {
        Product product = productRepository.findWithFoodTagsById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm");
        }
        Set<String> codes = product.getFoodTags().stream()
                .map(FoodTag::getCode)
                .collect(Collectors.toSet());
        if (codes.isEmpty()) {
            return new ProductIncompatibilityResponse(
                    productId, product.getName(), List.of(), List.of()
            );
        }
        List<FoodIncompatibility> activeRules = ruleRepository.findActiveTouchingTags(codes);
        Map<String, String> tagNames = tagRepository.findAllById(
                        activeRules.stream()
                                .flatMap(rule -> java.util.stream.Stream.of(rule.getTagA(), rule.getTagB()))
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(FoodTag::getCode, FoodTag::getName));

        List<ProductIncompatibilityResponse.Rule> rules = activeRules.stream()
                .flatMap(rule -> {
                    List<ProductIncompatibilityResponse.Rule> matched = new ArrayList<>(2);
                    if (codes.contains(rule.getTagA())) {
                        matched.add(toProductRule(rule, rule.getTagA(), rule.getTagB(), tagNames));
                    }
                    if (codes.contains(rule.getTagB())) {
                        matched.add(toProductRule(rule, rule.getTagB(), rule.getTagA(), tagNames));
                    }
                    return matched.stream();
                })
                .sorted(Comparator.comparing(ProductIncompatibilityResponse.Rule::matchedTag)
                        .thenComparing(ProductIncompatibilityResponse.Rule::incompatibleTag))
                .toList();
        return new ProductIncompatibilityResponse(
                productId,
                product.getName(),
                codes.stream().sorted().toList(),
                rules
        );
    }

    private FoodIncompatibility findRule(UUID id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy tắc"));
    }

    private void applyRule(
            FoodIncompatibility rule,
            FoodIncompatibilityWriteRequest request,
            String[] pair
    ) {
        rule.setTagA(pair[0]);
        rule.setTagB(pair[1]);
        rule.setSeverity(request.severity());
        rule.setTitle(request.title().trim());
        rule.setEffect(request.effect().trim());
        rule.setRecommendation(
                request.recommendation() == null || request.recommendation().isBlank()
                        ? null : request.recommendation().trim()
        );
        rule.setActive(request.isActive() == null || request.isActive());
    }

    private void requireTags(Collection<String> codes) {
        for (String code : codes) {
            if (!tagRepository.existsById(code)) {
                throw new ResourceNotFoundException("Không tìm thấy tag " + code);
            }
        }
    }

    private static String[] normalizePair(String first, String second) {
        String tagA = normalizeTag(first);
        String tagB = normalizeTag(second);
        if (tagA.equals(tagB)) {
            throw new BadRequestException("Hai tag trong quy tắc phải khác nhau");
        }
        return tagA.compareTo(tagB) < 0
                ? new String[]{tagA, tagB}
                : new String[]{tagB, tagA};
    }

    private static String normalizeTag(String code) {
        return code.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static void addConflictingProducts(
            List<IncompatibilityCheckResponse.ConflictingProduct> result,
            List<Product> products,
            String tag
    ) {
        if (products == null) {
            return;
        }
        products.forEach(product -> result.add(
                new IncompatibilityCheckResponse.ConflictingProduct(
                        product.getId(), product.getName(), product.getImageUrl(), tag
                )
        ));
    }

    private ProductFoodTagsResponse toProductTags(Product product) {
        return new ProductFoodTagsResponse(
                product.getId(),
                product.getName(),
                product.getFoodTags().stream()
                        .sorted(Comparator.comparing(FoodTag::getCode))
                        .map(FoodTagResponse::from)
                        .toList()
        );
    }

    private ProductIncompatibilityResponse.Rule toProductRule(
            FoodIncompatibility rule,
            String matchedTag,
            String incompatibleTag,
            Map<String, String> tagNames
    ) {
        return new ProductIncompatibilityResponse.Rule(
                rule.getId(),
                matchedTag,
                incompatibleTag,
                tagNames.get(incompatibleTag),
                rule.getSeverity(),
                rule.getTitle(),
                rule.getEffect(),
                rule.getRecommendation()
        );
    }
}
