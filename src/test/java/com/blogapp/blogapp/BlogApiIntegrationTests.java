package com.blogapp.blogapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BlogApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginAndFetchCurrentUser() throws Exception {
        String email = uniqueEmail();
        perform(post("/api/auth/register"), null,
                Map.of("name", "Alice", "email", email, "password", "password123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.password").doesNotExist());

        String token = login(email, "password123");
        perform(get("/api/users/me"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void rejectsDuplicateEmailWrongPasswordAndInvalidInput() throws Exception {
        String email = uniqueEmail();
        register("Bob", email);

        perform(post("/api/auth/register"), null, Map.of("name", "Bob2", "email", email, "password", "password123"))
                .andExpect(status().isConflict());
        perform(post("/api/auth/login"), null, Map.of("email", email, "password", "wrong-password"))
                .andExpect(status().isUnauthorized());
        perform(post("/api/auth/register"), null, Map.of("name", "", "email", "not-an-email", "password", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void protectedEndpointsRequireToken() throws Exception {
        perform(post("/api/posts"), null, Map.of("title", "t", "content", "c"))
                .andExpect(status().isUnauthorized());
        perform(get("/api/users/me"), "not-a-real-token", null)
                .andExpect(status().isUnauthorized());
        perform(get("/api/posts"), null, null)
                .andExpect(status().isOk());
    }

    @Test
    void postLifecycleWithOwnershipChecks() throws Exception {
        String author = register("Author", uniqueEmail());
        String other = register("Other", uniqueEmail());

        long postId = id(perform(post("/api/posts"), author, Map.of("title", "Hello Spring", "content", "First post"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author.name").value("Author")));

        perform(get("/api/posts/" + postId), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hello Spring"));
        perform(get("/api/posts?search=hello spring"), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + postId + ")]").exists());

        perform(put("/api/posts/" + postId), other, Map.of("title", "Hacked", "content", "x"))
                .andExpect(status().isForbidden());
        perform(put("/api/posts/" + postId), author, Map.of("title", "Updated", "content", "Edited"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));

        perform(delete("/api/posts/" + postId), other, null).andExpect(status().isForbidden());
        perform(delete("/api/posts/" + postId), author, null).andExpect(status().isNoContent());
        perform(get("/api/posts/" + postId), null, null).andExpect(status().isNotFound());
    }

    @Test
    void commentsOnPosts() throws Exception {
        String author = register("PostAuthor", uniqueEmail());
        String commenter = register("Commenter", uniqueEmail());
        String stranger = register("Stranger", uniqueEmail());

        long postId = id(perform(post("/api/posts"), author, Map.of("title", "Comments", "content", "Talk here")));
        long commentId = id(perform(post("/api/posts/" + postId + "/comments"), commenter, Map.of("content", "Nice!"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author.name").value("Commenter")));

        perform(get("/api/posts/" + postId + "/comments"), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].content").value("Nice!"));

        perform(put("/api/comments/" + commentId), stranger, Map.of("content", "Edit")).andExpect(status().isForbidden());
        perform(put("/api/comments/" + commentId), commenter, Map.of("content", "Very nice!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Very nice!"));

        // The post's author may moderate comments on their post
        perform(delete("/api/comments/" + commentId), stranger, null).andExpect(status().isForbidden());
        perform(delete("/api/comments/" + commentId), author, null).andExpect(status().isNoContent());

        perform(get("/api/posts/999999/comments"), null, null).andExpect(status().isNotFound());
    }

    @Test
    void categoriesAreAdminManaged() throws Exception {
        String admin = login("admin@test.com", "admin-password");
        String user = register("Regular", uniqueEmail());
        String name = "Java " + UUID.randomUUID();

        perform(post("/api/categories"), user, Map.of("name", name)).andExpect(status().isForbidden());
        long categoryId = id(perform(post("/api/categories"), admin, Map.of("name", name, "description", "All things Java"))
                .andExpect(status().isCreated()));
        perform(post("/api/categories"), admin, Map.of("name", name)).andExpect(status().isConflict());

        long postId = id(perform(post("/api/posts"), user,
                Map.of("title", "Categorized", "content", "c", "categoryId", categoryId)));
        perform(get("/api/posts?categoryId=" + categoryId), null, null)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].category.name").value(name));

        // Deleting a category keeps its posts
        perform(delete("/api/categories/" + categoryId), admin, null).andExpect(status().isNoContent());
        perform(get("/api/posts/" + postId), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").isEmpty());
    }

    @Test
    void usersManageOnlyTheirOwnAccount() throws Exception {
        String email = uniqueEmail();
        String token = register("Carol", email);
        String other = register("Dave", uniqueEmail());
        long carolId = id(perform(get("/api/users/me"), token, null));

        perform(get("/api/users"), token, null).andExpect(status().isForbidden());
        perform(get("/api/users"), login("admin@test.com", "admin-password"), null).andExpect(status().isOk());

        perform(put("/api/users/" + carolId), other, Map.of("name", "X", "email", uniqueEmail()))
                .andExpect(status().isForbidden());
        perform(put("/api/users/" + carolId), token, Map.of("name", "Carol B", "email", email, "bio", "Writer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Writer"));

        // Deleting a user removes their posts and comments too
        long postId = id(perform(post("/api/posts"), token, Map.of("title", "Mine", "content", "c")));
        perform(post("/api/posts/" + postId + "/comments"), token, Map.of("content", "self comment"));
        perform(post("/api/posts/" + postId + "/comments"), other, Map.of("content", "other comment"));

        perform(delete("/api/users/" + carolId), token, null).andExpect(status().isNoContent());
        perform(get("/api/posts/" + postId), null, null).andExpect(status().isNotFound());
        perform(get("/api/users/me"), token, null).andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private ResultActions perform(MockHttpServletRequestBuilder request, String token, Object body) throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        return mockMvc.perform(request);
    }

    private String register(String name, String email) throws Exception {
        return json(perform(post("/api/auth/register"), null,
                Map.of("name", name, "email", email, "password", "password123"))
                .andExpect(status().isCreated())).get("token").asText();
    }

    private String login(String email, String password) throws Exception {
        return json(perform(post("/api/auth/login"), null, Map.of("email", email, "password", password))
                .andExpect(status().isOk())).get("token").asText();
    }

    private long id(ResultActions result) throws Exception {
        return json(result).get("id").asLong();
    }

    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
