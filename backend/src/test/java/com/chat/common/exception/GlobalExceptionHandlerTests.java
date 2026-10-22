package com.chat.common.exception;

import com.chat.common.config.ValidationConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GlobalExceptionHandlerTests.TestController.class)
@Import({ValidationConfig.class, GlobalExceptionHandlerTests.TestController.class})
class GlobalExceptionHandlerTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void validationUsesVietnameseByDefaultAndDoesNotExposeRejectedValues() throws Exception {
        mvc.perform(post("/test/input").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\", \"password\":\"secret-password\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.path").value("/test/input"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Không được để trống"))
                .andExpect(content().string(not(containsString("secret-password"))));
    }

    @Test
    void validationSupportsEnglish() throws Exception {
        mvc.perform(post("/test/input").header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request data"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Must not be blank"));
    }

    @Test
    void malformedJsonDoesNotLeakParserDetails() throws Exception {
        mvc.perform(post("/test/input").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": secret-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andExpect(content().string(not(containsString("secret-token"))));
    }

    @ParameterizedTest
    @CsvSource({"400,VALIDATION_ERROR", "401,UNAUTHENTICATED", "403,FORBIDDEN",
            "404,NOT_FOUND", "409,CONFLICT", "413,PAYLOAD_TOO_LARGE", "415,UNSUPPORTED_MEDIA_TYPE"})
    void businessErrorsUseStableStatusAndCode(int httpStatus, String code) throws Exception {
        mvc.perform(get("/test/business").param("status", Integer.toString(httpStatus)))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.status").value(httpStatus))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andExpect(content().string(not(containsString("missing.message.key"))))
                .andExpect(jsonPath("$.path").value("/test/business"));
    }

    @Test
    void missingAndInvalidQueryParametersStayClientErrors() throws Exception {
        for (String url : new String[]{"/test/business", "/test/business?status=abc"}) {
            mvc.perform(get(url))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void unknownRouteUsesErrorContract() throws Exception {
        mvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/does-not-exist"));
    }

    @Test
    void wrongMethodRetainsHttpStatusAndAllowHeader() throws Exception {
        mvc.perform(get("/test/input"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("POST")))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void wrongContentTypeUsesUnsupportedMediaType() throws Exception {
        mvc.perform(post("/test/input").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void unexpectedFailureDoesNotLeakImplementationDetails() throws Exception {
        mvc.perform(get("/test/failure").queryParam("token", "private-token"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Lỗi nội bộ hệ thống"))
                .andExpect(jsonPath("$.path").value("/test/failure"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("private-token"))))
                .andExpect(content().string(not(containsString("database-password"))));
    }

    @Test
    void successfulResponseIsNotWrapped() throws Exception {
        mvc.perform(post("/test/input").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"An\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("An"))
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    // Test-only controller: no example endpoints are added to the application.
    @RestController
    static class TestController {
        @PostMapping("/test/input")
        Input input(@Valid @RequestBody Input input) {
            return input;
        }

        @GetMapping("/test/business")
        void business(@RequestParam int status) {
            throw new BusinessException(HttpStatus.valueOf(status), "missing.message.key");
        }

        @GetMapping("/test/failure")
        void failure() {
            throw new IllegalStateException("database-password");
        }
    }

    record Input(@NotBlank(message = "{validation.notBlank}") String name, String password) {
    }
}
