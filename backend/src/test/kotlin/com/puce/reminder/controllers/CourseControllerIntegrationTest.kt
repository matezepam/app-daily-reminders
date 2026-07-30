package com.puce.reminder.controllers

import com.puce.reminder.entities.Course
import com.puce.reminder.repositories.CourseMembershipRepository
import com.puce.reminder.repositories.CourseRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:courses;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.cognito.region=us-east-1",
        "app.cognito.user-pool-id=us-east-1_test",
        "app.cognito.client-id=daily-reminder-client",
    ],
)
@AutoConfigureMockMvc
@Transactional
class CourseControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val courses: CourseRepository,
    @Autowired private val memberships: CourseMembershipRepository,
) {
    @Test
    fun `teacher creates a course with a unique join code`() {
        mockMvc.perform(
            post("/api/v1/courses")
                .with(
                    jwt()
                        .jwt { token -> token.subject("teacher-id") }
                        .authorities(SimpleGrantedAuthority("ROLE_TEACHER")),
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Matematicas II","description":"Periodo actual"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Matematicas II"))
            .andExpect(jsonPath("$.ownerUserId").value("teacher-id"))
            .andExpect(jsonPath("$.joinCode").value(org.hamcrest.Matchers.matchesPattern("MAT-[A-Z2-9]{5}")))
    }

    @Test
    fun `student cannot create a course`() {
        mockMvc.perform(
            post("/api/v1/courses")
                .with(
                    jwt()
                        .jwt { token -> token.subject("student-id") }
                        .authorities(SimpleGrantedAuthority("ROLE_STUDENT")),
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Curso no permitido"}"""),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `student joins a course using its code`() {
        val course = courses.save(
            Course(name = "Fisica", joinCode = "FIS-82KLM", ownerUserId = "teacher-id"),
        )

        mockMvc.perform(
            post("/api/v1/courses/join")
                .with(
                    jwt()
                        .jwt { token -> token.subject("student-id") }
                        .authorities(SimpleGrantedAuthority("ROLE_STUDENT")),
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"code":"fis-82klm"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(course.id))
            .andExpect(jsonPath("$.joinCode").value("FIS-82KLM"))

        assertTrue(memberships.existsByCourseIdAndStudentUserId(requireNotNull(course.id), "student-id"))
    }

    @Test
    fun `student cannot join the same course twice`() {
        courses.save(Course(name = "Quimica", joinCode = "QUI-92KLM", ownerUserId = "teacher-id"))
        val request = post("/api/v1/courses/join")
            .with(
                jwt()
                    .jwt { token -> token.subject("student-id") }
                    .authorities(SimpleGrantedAuthority("ROLE_STUDENT")),
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"code":"QUI-92KLM"}""")

        mockMvc.perform(request).andExpect(status().isOk)
        mockMvc.perform(request)
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Ya perteneces a esta clase"))
    }

    @Test
    fun `unknown course code returns not found`() {
        mockMvc.perform(
            post("/api/v1/courses/join")
                .with(
                    jwt()
                        .jwt { token -> token.subject("student-id") }
                        .authorities(SimpleGrantedAuthority("ROLE_STUDENT")),
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"code":"XXX-99999"}"""),
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `user lists owned and joined courses`() {
        courses.save(Course(name = "Algebra", joinCode = "ALG-82KLM", ownerUserId = "teacher-id"))
        val joinedCourse = courses.save(Course(name = "Biologia", joinCode = "BIO-82KLM", ownerUserId = "other-teacher"))

        mockMvc.perform(
            post("/api/v1/courses/join")
                .with(
                    jwt()
                        .jwt { token -> token.subject("teacher-id") }
                        .authorities(SimpleGrantedAuthority("ROLE_STUDENT")),
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"code":"BIO-82KLM"}"""),
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/v1/courses/me")
                .with(
                    jwt()
                        .jwt { token -> token.subject("teacher-id") }
                        .authorities(SimpleGrantedAuthority("ROLE_TEACHER")),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Algebra"))
            .andExpect(jsonPath("$[0].ownedByMe").value(true))
            .andExpect(jsonPath("$[1].id").value(joinedCourse.id))
            .andExpect(jsonPath("$[1].ownedByMe").value(false))
    }
}
