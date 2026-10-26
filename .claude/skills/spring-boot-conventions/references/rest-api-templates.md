# コード雛形

パッケージ名 `com.example.app` は実プロジェクトのものに置き換えること。
ここに示す構成は以下を想定している。

```
com.example.app
├── common/exception/   ErrorCode, BusinessException とその派生, GlobalExceptionHandler
└── user/               UserController, UserService, UserRepository, User(Entity), dto/
```

## 目次
1. pom.xml の依存関係
2. ErrorCode
3. 例外クラス
4. GlobalExceptionHandler
5. DTO（record）
6. Controller
7. Service
8. Controller テスト

---

## 1. pom.xml の依存関係

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

`application.properties`:

```properties
spring.mvc.problemdetails.enabled=true
```

## 2. ErrorCode

```java
package com.example.app.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation Failed"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed Request"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource Not Found"),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "Duplicate Resource"),
    OPTIMISTIC_LOCK_CONFLICT(HttpStatus.CONFLICT, "Conflict"),
    BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "Business Rule Violation"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");

    private final HttpStatus status;
    private final String title;

    ErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() { return status; }
    public String title() { return title; }
}
```

## 3. 例外クラス

```java
package com.example.app.common.exception;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() { return errorCode; }
}
```

```java
package com.example.app.common.exception;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resourceName, Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
              "%sが見つかりません（id=%s）".formatted(resourceName, id));
    }
}
```

```java
package com.example.app.common.exception;

public class ConflictException extends BusinessException {
    public ConflictException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
    }
}
```

```java
package com.example.app.common.exception;

public class BusinessRuleViolationException extends BusinessException {
    public BusinessRuleViolationException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }
}
```

## 4. GlobalExceptionHandler

```java
package com.example.app.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---- 業務例外 ----
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex, HttpServletRequest req) {
        log.warn("[{}] {}", ex.getErrorCode(), ex.getMessage());
        return build(ex.getErrorCode(), ex.getMessage(), req.getRequestURI(), null);
    }

    // ---- パス・クエリパラメータの検証（@Validated） ----
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest req) {
        List<Map<String, String>> errors = ex.getConstraintViolations().stream()
                .map(v -> Map.of(
                        "field", lastNode(v.getPropertyPath().toString()),
                        "message", v.getMessage()))
                .toList();
        log.warn("[{}] {}", ErrorCode.VALIDATION_ERROR, errors);
        return build(ErrorCode.VALIDATION_ERROR, "入力内容に誤りがあります", req.getRequestURI(), errors);
    }

    // ---- 楽観ロック ----
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                                              HttpServletRequest req) {
        log.warn("[{}] {}", ErrorCode.OPTIMISTIC_LOCK_CONFLICT, ex.getMessage());
        return build(ErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                "他のユーザーによって更新されています。再読み込みしてからやり直してください",
                req.getRequestURI(), null);
    }

    // ---- 想定外（最後の砦）：内部情報は返さない ----
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error at {}", req.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, "サーバー内部でエラーが発生しました", req.getRequestURI(), null);
    }

    // ---- @Valid @RequestBody の検証エラー ----
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage() == null ? "不正な値です" : fe.getDefaultMessage()))
                .toList();
        log.warn("[{}] {}", ErrorCode.VALIDATION_ERROR, errors);
        return asObject(build(ErrorCode.VALIDATION_ERROR, "入力内容に誤りがあります", path(request), errors));
    }

    // ---- JSON が壊れている・型が合わない ----
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        log.warn("[{}] {}", ErrorCode.MALFORMED_REQUEST, ex.getMostSpecificCause().getMessage());
        return asObject(build(ErrorCode.MALFORMED_REQUEST,
                "リクエストの形式が正しくありません", path(request), null));
    }

    // ---- 共通の組み立て ----
    private ResponseEntity<ProblemDetail> build(ErrorCode code, String detail, String path,
                                                List<Map<String, String>> errors) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(code.status(), detail);
        pd.setTitle(code.title());
        pd.setInstance(URI.create(path));
        pd.setProperty("code", code.name());
        pd.setProperty("timestamp", OffsetDateTime.now());
        if (errors != null) {
            pd.setProperty("errors", errors);
        }
        return ResponseEntity.status(code.status()).body(pd);
    }

    private static ResponseEntity<Object> asObject(ResponseEntity<ProblemDetail> res) {
        return ResponseEntity.status(res.getStatusCode()).body(res.getBody());
    }

    private static String path(WebRequest request) {
        return (request instanceof ServletWebRequest swr) ? swr.getRequest().getRequestURI() : "";
    }

    private static String lastNode(String propertyPath) {
        int i = propertyPath.lastIndexOf('.');
        return i >= 0 ? propertyPath.substring(i + 1) : propertyPath;
    }
}
```

補足: `ResponseEntityExceptionHandler` が処理する他の標準例外（405 Method Not Allowed、415 Unsupported Media Type など）は、Spring が自動的に ProblemDetail 形式で返す。それらにも `code` を付けたい場合は `createResponseEntity` をオーバーライドして拡張プロパティを足す。

## 5. DTO（record）

```java
package com.example.app.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "名前は必須です")
        @Size(max = 50, message = "名前は50文字以内で入力してください")
        String name,

        @NotBlank(message = "メールアドレスは必須です")
        @Email(message = "メールアドレスの形式が正しくありません")
        String email
) {}
```

```java
package com.example.app.user.dto;

import com.example.app.user.User;
import java.time.OffsetDateTime;

public record UserResponse(Long id, String name, String email, OffsetDateTime createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
```

## 6. Controller

```java
package com.example.app.user;

import com.example.app.user.dto.CreateUserRequest;
import com.example.app.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = userService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

## 7. Service

```java
package com.example.app.user;

import com.example.app.common.exception.ConflictException;
import com.example.app.common.exception.ResourceNotFoundException;
import com.example.app.user.dto.CreateUserRequest;
import com.example.app.user.dto.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("ユーザー", id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("このメールアドレスは既に登録されています");
        }
        User saved = userRepository.save(new User(request.name(), request.email()));
        return UserResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("ユーザー", id);
        }
        userRepository.deleteById(id);
    }
}
```

## 8. Controller テスト

```java
package com.example.app.user;

import com.example.app.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // 3.3以前は @MockBean
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean UserService userService;

    @Test
    void 存在しないIDは404とRESOURCE_NOT_FOUNDを返す() throws Exception {
        when(userService.findById(999L)).thenThrow(new ResourceNotFoundException("ユーザー", 999L));

        mockMvc.perform(get("/api/v1/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void メール形式が不正なら400とerrorsを返す() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"山田太郎","email":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }
}
```
