package com.haui.UrbanMart.repository;

import com.haui.UrbanMart.dto.request.ReviewType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tim kiem gop danh gia don hang + danh gia san pham (UNION ALL) de chi nhanh/admin
 * xem chung mot danh sach, phan trang va sap xep theo thoi gian tao.
 * Voi danh gia don hang, "rating" duoc hieu la service_rating.
 */
@Repository
public class ReviewSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public record Filter(
            UUID branchId,
            ReviewType type,
            Integer rating,
            OffsetDateTime from,
            OffsetDateTime to,
            String keyword
    ) {
    }

    public record Row(
            UUID id,
            String reviewType,
            UUID orderId,
            String orderCode,
            UUID customerId,
            String customerName,
            String customerPhone,
            UUID branchId,
            String branchName,
            UUID productId,
            String productName,
            Integer rating,
            Integer deliveryRating,
            Integer serviceRating,
            String comment,
            List<String> images,
            OffsetDateTime createdAt
    ) {
    }

    public Page<Row> search(Filter filter, Pageable pageable) {
        Map<String, Object> params = new LinkedHashMap<>();
        String union = buildUnion(filter, params);

        Query dataQuery = entityManager.createNativeQuery(
                "SELECT * FROM (" + union + ") r ORDER BY r.created_at DESC, r.id ASC"
        );
        params.forEach(dataQuery::setParameter);
        dataQuery.setFirstResult(Math.toIntExact(pageable.getOffset()));
        dataQuery.setMaxResults(pageable.getPageSize());

        Query countQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM (" + union + ") r"
        );
        params.forEach(countQuery::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();
        return new PageImpl<>(rows.stream().map(this::toRow).toList(), pageable, total);
    }

    private String buildUnion(Filter filter, Map<String, Object> params) {
        ReviewType type = filter.type() == null ? ReviewType.ALL : filter.type();
        List<String> parts = new ArrayList<>();
        if (type != ReviewType.PRODUCT) {
            parts.add(orderReviewSelect(filter, params));
        }
        if (type != ReviewType.ORDER) {
            parts.add(productReviewSelect(filter, params));
        }
        return String.join(" UNION ALL ", parts);
    }

    private String orderReviewSelect(Filter filter, Map<String, Object> params) {
        List<String> where = commonWhere("orv", filter, params);
        if (filter.rating() != null) {
            where.add("orv.service_rating = :rating");
        }
        if (hasKeyword(filter)) {
            where.add("(LOWER(o.order_code) LIKE :keyword OR LOWER(u.full_name) LIKE :keyword)");
        }
        return """
                SELECT orv.id AS id,
                       'ORDER' AS review_type,
                       o.id AS order_id,
                       o.order_code AS order_code,
                       u.id AS customer_id,
                       u.full_name AS customer_name,
                       u.phone AS customer_phone,
                       b.id AS branch_id,
                       b.name AS branch_name,
                       CAST(NULL AS uuid) AS product_id,
                       CAST(NULL AS varchar) AS product_name,
                       orv.service_rating AS rating,
                       orv.delivery_rating AS delivery_rating,
                       orv.service_rating AS service_rating,
                       orv.comment AS comment,
                       (SELECT string_agg(img, E'\\n') FROM jsonb_array_elements_text(orv.images) AS img) AS images,
                       orv.created_at AS created_at
                FROM order_reviews orv
                JOIN orders o ON o.id = orv.order_id
                JOIN users u ON u.id = orv.customer_id
                JOIN branches b ON b.id = orv.branch_id
                """ + whereClause(where);
    }

    private String productReviewSelect(Filter filter, Map<String, Object> params) {
        List<String> where = commonWhere("pr", filter, params);
        if (filter.rating() != null) {
            where.add("pr.rating = :rating");
        }
        if (hasKeyword(filter)) {
            where.add("(LOWER(o.order_code) LIKE :keyword OR LOWER(u.full_name) LIKE :keyword"
                    + " OR LOWER(p.name) LIKE :keyword)");
        }
        return """
                SELECT pr.id AS id,
                       'PRODUCT' AS review_type,
                       o.id AS order_id,
                       o.order_code AS order_code,
                       u.id AS customer_id,
                       u.full_name AS customer_name,
                       u.phone AS customer_phone,
                       b.id AS branch_id,
                       b.name AS branch_name,
                       p.id AS product_id,
                       CAST(p.name AS varchar) AS product_name,
                       pr.rating AS rating,
                       CAST(NULL AS integer) AS delivery_rating,
                       CAST(NULL AS integer) AS service_rating,
                       pr.comment AS comment,
                       (SELECT string_agg(img, E'\\n') FROM jsonb_array_elements_text(pr.images) AS img) AS images,
                       pr.created_at AS created_at
                FROM product_reviews pr
                JOIN orders o ON o.id = pr.order_id
                JOIN users u ON u.id = pr.customer_id
                JOIN branches b ON b.id = pr.branch_id
                JOIN products p ON p.id = pr.product_id
                """ + whereClause(where);
    }

    private List<String> commonWhere(String alias, Filter filter, Map<String, Object> params) {
        List<String> where = new ArrayList<>();
        if (filter.branchId() != null) {
            where.add(alias + ".branch_id = :branchId");
            params.put("branchId", filter.branchId());
        }
        if (filter.rating() != null) {
            params.put("rating", filter.rating());
        }
        if (filter.from() != null) {
            where.add(alias + ".created_at >= :fromTime");
            params.put("fromTime", filter.from());
        }
        if (filter.to() != null) {
            where.add(alias + ".created_at < :toTime");
            params.put("toTime", filter.to());
        }
        if (hasKeyword(filter)) {
            params.put("keyword", "%" + filter.keyword().trim().toLowerCase() + "%");
        }
        return where;
    }

    private boolean hasKeyword(Filter filter) {
        return filter.keyword() != null && !filter.keyword().isBlank();
    }

    private String whereClause(List<String> conditions) {
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    private Row toRow(Object[] columns) {
        return new Row(
                toUuid(columns[0]),
                (String) columns[1],
                toUuid(columns[2]),
                (String) columns[3],
                toUuid(columns[4]),
                (String) columns[5],
                (String) columns[6],
                toUuid(columns[7]),
                (String) columns[8],
                toUuid(columns[9]),
                (String) columns[10],
                toInteger(columns[11]),
                toInteger(columns[12]),
                toInteger(columns[13]),
                (String) columns[14],
                toImages(columns[15]),
                toOffsetDateTime(columns[16])
        );
    }

    private static UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
    }

    private static Integer toInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static List<String> toImages(Object value) {
        if (value == null || value.toString().isEmpty()) {
            return List.of();
        }
        return Arrays.asList(value.toString().split("\n"));
    }

    private static OffsetDateTime toOffsetDateTime(Object value) {
        return switch (value) {
            case null -> null;
            case OffsetDateTime offsetDateTime -> offsetDateTime;
            case Instant instant -> instant.atOffset(ZoneOffset.UTC);
            case ZonedDateTime zonedDateTime -> zonedDateTime.toOffsetDateTime();
            case Timestamp timestamp -> timestamp.toInstant().atOffset(ZoneOffset.UTC);
            default -> throw new IllegalStateException(
                    "Kieu thoi gian khong ho tro: " + value.getClass()
            );
        };
    }
}
