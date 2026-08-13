package com.enterprise.lab.todo;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// REST API phục vụ thao tác Todo qua HAProxy và Nginx.
@RestController
@RequestMapping("/api/todos")
public class TodoController {
    private final TodoRepository repository;

    public TodoController(TodoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Todo> list() {
        return repository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Todo create(@Valid @RequestBody Todo todo) {
        return repository.save(new Todo(todo.getTitle()));
    }

    @PutMapping("/{id}")
    public Todo update(@PathVariable Long id, @Valid @RequestBody Todo request) {
        Todo todo = repository.findById(id).orElseThrow();
        todo.setTitle(request.getTitle());
        todo.setCompleted(request.isCompleted());
        return repository.save(todo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
