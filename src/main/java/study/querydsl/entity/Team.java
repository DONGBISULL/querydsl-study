package study.querydsl.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자를 만들어줌
@ToString(of={"id" , "name"})
@SuperBuilder
public class Team extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "team_id")
    private Long id;

    private String name;

    @OneToMany(mappedBy = "team" )
    private List<Member> members = new ArrayList<>();

    public Team(String name) {
        this.name = name;
    }

}
