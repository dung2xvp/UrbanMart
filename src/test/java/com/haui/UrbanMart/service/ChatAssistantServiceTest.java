package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.response.ChatMessageResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatAssistantServiceTest {

    @Test
    void onlyUniqueIdsFromBackendCandidatesAreAccepted() {
        UUID candidateId = UUID.randomUUID();
        UUID unknownId = UUID.randomUUID();

        List<UUID> result = ChatAssistantService.filterModelIds(
                java.util.Arrays.asList(
                        unknownId,
                        candidateId,
                        candidateId,
                        null
                ),
                Set.of(candidateId),
                5
        );

        assertEquals(List.of(candidateId), result);
    }

    @Test
    void modelSuggestionsRespectConfiguredMaximum() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        UUID thirdId = UUID.randomUUID();

        List<UUID> result = ChatAssistantService.filterModelIds(
                List.of(firstId, secondId, thirdId),
                Set.of(firstId, secondId, thirdId),
                2
        );

        assertEquals(List.of(firstId, secondId), result);
    }

    @Test
    void modelCannotForceSuggestionsWhenNoCandidatesExist() {
        UUID unknownId = UUID.randomUUID();

        List<UUID> result = ChatAssistantService.filterModelIds(
                List.of(unknownId),
                Set.of(),
                5
        );

        assertEquals(List.of(), result);
    }

    @Test
    void replyTextIsTrimmedAndControlCharactersAreRemoved() {
        assertEquals(
                "Xin chào!",
                ChatAssistantService.sanitizeReply(" \tXin chào!\n")
        );
    }

    @Test
    void voucherOnlyReplyUsesVoucherRecommendationType() {
        assertEquals("RECOMMEND_VOUCHERS", ChatAssistantService.responseType(false, true));
    }

    @Test
    void directSearchExtractsProductKeywordsFromNaturalVietnameseRequest() {
        assertEquals(
                List.of("sua"),
                ChatAssistantService.extractDirectProductSearchTerms(
                        "Gợi ý sản phẩm sữa có trong cửa hàng"
                )
        );
    }

    @Test
    void directSearchKeepsRecipeTermsAndDropsIntentWordsAndQuantities() {
        assertEquals(
                List.of("lau", "thai"),
                ChatAssistantService.extractDirectProductSearchTerms(
                        "Gợi ý nguyên liệu lẩu Thái cho 4 người ăn"
                )
        );
    }

    @Test
    void directFallbackSelectsOnlyProductsMatchingSearchTerms() {
        UUID milkId = UUID.randomUUID();
        UUID juiceId = UUID.randomUUID();
        List<ChatAssistantService.ProductCandidate> candidates = List.of(
                candidate(milkId, "MILK-01", "Sữa tươi 1L", "sữa tươi"),
                candidate(juiceId, "JUICE-01", "Nước cam", "nước cam")
        );

        assertEquals(
                List.of(milkId),
                ChatAssistantService.selectDirectProductMatches(candidates, List.of("sua"))
        );
    }

    @Test
    void malformedAiOutputCanFallbackToVerifiedMatchingProducts() {
        UUID milkId = UUID.randomUUID();
        List<ChatAssistantService.ProductCandidate> candidates = List.of(
                candidate(milkId, "MILK-01", "Sữa tươi 1L", "sữa tươi")
        );

        ChatAssistantService.ModelResponse fallback =
                ChatAssistantService.createCatalogFallback(
                        "Gợi ý sản phẩm sữa có trong cửa hàng",
                        candidates
                );

        assertEquals(List.of(milkId), fallback.productIds());
        assertEquals(List.of(), fallback.voucherIds());
        assertEquals(true, fallback.replyText().contains("AI tạm thời"));
    }

    @Test
    void malformedAiOutputHasNoSuccessFallbackWithoutMatchingCatalogData() {
        assertEquals(
                null,
                ChatAssistantService.createCatalogFallback(
                        "Gợi ý sản phẩm sữa",
                        List.of()
                )
        );
    }

    @Test
    void directSearchCanMatchBrandOrDescriptionWhenNameOmitsKeyword() {
        UUID productId = UUID.randomUUID();

        assertEquals(
                List.of(productId),
                ChatAssistantService.selectDirectProductMatches(
                        List.of(candidate(
                                productId,
                                "SKU-01",
                                "Dinh dưỡng tiệt trùng",
                                "dinh dưỡng tiệt trùng vinamilk sữa"
                        )),
                        List.of("sua")
                )
        );
    }

    @Test
    void directSearchIsAccentInsensitiveForVietnameseCatalogText() {
        assertEquals(
                List.of("sua", "dau"),
                ChatAssistantService.extractDirectProductSearchTerms(
                        "Gợi ý sản phẩm sữa đậu"
                )
        );
        UUID productId = UUID.randomUUID();
        assertEquals(
                List.of(productId),
                ChatAssistantService.selectDirectProductMatches(
                        List.of(candidate(
                                productId,
                                "SKU-01",
                                "Sua tuoi",
                                "sua tuoi"
                        )),
                        List.of("sua")
                )
        );
    }

    private ChatAssistantService.ProductCandidate candidate(
            UUID id,
            String sku,
            String name,
            String searchableText
    ) {
        return new ChatAssistantService.ProductCandidate(
                new ChatMessageResponse.SuggestedProduct(
                        id, sku, name, null, null, null, "Hộp", 5, true
                ),
                searchableText
        );
    }
}
