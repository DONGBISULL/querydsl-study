package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter // 실무에선 사용에 주의
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자를 만들어줌
@ToString(of = {"id", "username", "age"}) // 자신이 소유한 필드에 대해서만 toString 을 만들어야 함 그외 것들을 가지고 있을 경우 순환 오류 !!
public class Member {
    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String username;

    private int age;

    private String grade;

    private String mName;

    // 연관관계 주인!
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id") //외래키 이름 빠트린 부분이므로 꼭 내용 확인
    private Team team;

    public Member(String username) {
        this(username, 0);
    }

    public Member(String username, int age) {
        this(username, age, null);
    }

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }
    }

    public void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }
}
