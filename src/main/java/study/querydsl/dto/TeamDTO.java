package study.querydsl.dto;

import lombok.*;
import study.querydsl.entity.Member;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamDTO {
    private Long id;
    private String name;
    private List<Member> member;
}
