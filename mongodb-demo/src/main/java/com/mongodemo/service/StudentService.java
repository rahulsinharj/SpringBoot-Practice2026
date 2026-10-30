package com.mongodemo.service;

import com.mongodemo.models.Student;
import com.mongodemo.repo.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public Student addStudent(Student student) {
        return this.studentRepository.save(student);
    }

    public List<Student> getAllStudents() {
        return this.studentRepository.findAll();
    }
}
