package com.example.demo.api.member.controller;

import com.example.demo.api.member.entity.Member;
import com.example.demo.api.member.repository.MemberRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "Member 관련 API 입니다.")
@RestController
@RequestMapping("/api/v2/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberRepository memberRepository;



    @PostMapping
    public ResponseEntity<String> addMember(@RequestParam String name) {
        Member member = new Member(name);

        memberRepository.save(member);

        return ResponseEntity.ok("가입 성공");

    }
}
