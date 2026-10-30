package com.mongodemo.controller;

import com.mongodemo.models.Student;
import com.mongodemo.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/students")
public class StudentController {

    Logger logger = Logger.getLogger(StudentController.class.getName());

    @Autowired
    private StudentService studentService;

    @GetMapping("/")
    public ResponseEntity<?> getAllStudents() {
        // Implement logic to retrieve all students from the database

        List<Student> allStudents = this.studentService.getAllStudents();
        logger.info("All Students: " + allStudents);
        return ResponseEntity.ok(allStudents);
    }

    @PostMapping("/")
    public ResponseEntity<?> addStudent(@RequestBody Student student) {
        // Implement logic to add a new student to the database
        Student res = this.studentService.addStudent(student);
        logger.info("Added Student: " + res);
        return ResponseEntity.ok(res);
    }

}
