package com.enterprise.lab.todo;

import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data tự sinh CRUD repository cho bảng todo.
public interface TodoRepository extends JpaRepository<Todo, Long> {
}
