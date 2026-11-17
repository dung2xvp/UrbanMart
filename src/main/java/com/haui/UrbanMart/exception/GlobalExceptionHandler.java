package com.haui.UrbanMart.exception;

import com.haui.UrbanMart.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiResponse<Void>> handleTooManyRequests(TooManyRequestsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // Bat loi tu @Valid tren cac DTO request (vd: UserCreationRequest) -
    // gom loi cua tung field lai thanh 1 Map de frontend biet chinh xac
    // field nao sai, thay vi 1 cau thong bao chung chung.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                errors.put(fieldError.getField(), validationMessage(fieldError))
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>("error", "Dữ liệu gửi lên không hợp lệ", errors));
    }

    private String validationMessage(FieldError fieldError) {
        String message = fieldError.getDefaultMessage();
        if (message == null || containsDiacritics(message)) {
            return message;
        }

        return switch (fieldError.getCode() == null ? "" : fieldError.getCode()) {
            case "NotNull", "NotBlank", "NotEmpty" -> "Trường này không được để trống";
            case "Size" -> "Độ dài hoặc số lượng phần tử không nằm trong giới hạn cho phép";
            case "Min", "DecimalMin" -> "Giá trị nhỏ hơn mức tối thiểu cho phép";
            case "Max", "DecimalMax" -> "Giá trị vượt quá mức tối đa cho phép";
            case "Digits" -> "Giá trị không đúng định dạng số cho phép";
            case "Email" -> "Địa chỉ email không hợp lệ";
            case "Pattern" -> "Giá trị không đúng định dạng yêu cầu";
            case "Positive" -> "Giá trị phải lớn hơn 0";
            case "PositiveOrZero" -> "Giá trị không được nhỏ hơn 0";
            case "Future" -> "Thời gian phải nằm trong tương lai";
            case "FutureOrPresent" -> "Thời gian không được ở quá khứ";
            case "Past" -> "Thời gian phải nằm trong quá khứ";
            case "PastOrPresent" -> "Thời gian không được ở tương lai";
            default -> "Giá trị không hợp lệ";
        };
    }

    private boolean containsDiacritics(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .codePoints()
                .anyMatch(codePoint ->
                        Character.getType(codePoint) == Character.NON_SPACING_MARK
                );
    }

    // Bat tat ca exception con lai chua duoc xu ly rieng - tranh lo ra
    // stack trace/thong tin noi bo cho nguoi dung, luon tra ve 500 gon gang.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error(
                "Unhandled exception while handling {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau"));
    }
}