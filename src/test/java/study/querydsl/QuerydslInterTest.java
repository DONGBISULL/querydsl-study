package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.MemeberSearchDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslInterTest {
    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("TeamA");
        Team teamB = new Team("TeamB");

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    /**
     * 프로젝션 결과 반환
     * => 열 단위 조회
     * 프로젝션 대상이 하나인 경우 타입 지정 가능
     */
    @Test
    public void simpleProjectionTest() throws Exception {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();
    }

    /**
     * 여러개 필드 조회 시
     */
    @Test
    public void tupleProjection() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("member username = " + username);
            System.out.println("member age = " + age);
        }
    }

    /**
     * JPA 에서 DTO 조회할 때 new 명령어를 사용하여 조회해야 함
     */
    @Test
    public void findDtoByJPQL() throws Exception {
        String jpqlQuery = "select new study.querydsl.dto.MemberDto(m.username , m.age) from Member m";
        List<MemberDto> result = em.createQuery(jpqlQuery, MemberDto.class)
                .getResultList();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 1. 프로퍼티 접근 방법
     * Setter를 이용하여 조회
     * => setter가 무조건 있어야 함
     */
    @Test
    public void findDtoBySetter() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 2. 필드 직접 접근
     * => getter , setter 없어도 자동으로 세팅 가능
     */
    @Test
    public void findDtoByField() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 3. 생성자 방식
     */
    @Test
    public void findDtoByConstructor() throws Exception {
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age)) // 생성자에 없는 데이터를 추가 조회하려하면 런타임 오류 발생
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findUserDto() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * @QueryProjection 가 실무에서 고민되는 이유
     * => q 타입을 생성해야 함!!
     * => 의존성에 대한 문제 발생
     * querydsl을 빼게 될 경우 에러 발생
     * => dto 자체가 순수하지 않게 됨
     * => 필드 방식을 지향하는게 나에겐 더 좋아 보인다!!
     */
    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 동적쿼리 해결하는 방법
     * 1. BooleanBuilder
     * 2. Where 다중 파라미터 사용
     * */

    /**
     * 동적 쿼리 booleanBuilder 사용
     * => where에서 null은 그냥 무시하니까 아닐 경우 null 리턴하면 되나???
     */
    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }
        return queryFactory
                .select(member)
                .from(member)
                .where(builder)
                .fetch();
    }

    /**
     * 2. 동적 쿼리 다중 where 절 사용
     * BooleanExpression 으로 분리하여 다른 쿼리에서 재활용할 수 있도록 함
     * NULL 체크 해줘야 함!!
     */
    @Test
    public void dynamicQuery_WhereParam() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond),
                        ageEq(ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        if (usernameCond == null) {
            return null;
        }
        return member.username.eq(usernameCond);
        //        간단할 경우 삼항 연산자로 리턴
        //        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    //    광고 상태 isValid , 날짜 IN : isServicable

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    /**
     * 실무에서 사용하는 예시
     */
    private List<MemberDto> searchMember(MemeberSearchDto searchDto) {
        List<MemberDto> memberList = queryFactory
                .select(
                        Projections.fields(
                                MemberDto.class,
                                member.username,
                                member.age
                        )
                )
                .from(member)
                .where(
                        eqMemberName(searchDto.getUsername())
                )
                .fetch();
        return memberList;

    }

    private BooleanExpression eqMemberGrade(String type) {
        if (StringUtils.isEmpty(type)) {
            return null;
        }
        return member.grade.eq(type);
    }

    private BooleanBuilder eqMemberName(String text) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(member.username.contains(text));//%text%
        builder.or(member.mName.contains(text));
        return builder;
    }

    /**
     * 업데이트 벌크
     * => db에 바로 전송
     * => 영속성 컨텍스트와 데이터가 달라짐!!
     */
    @Test
    public void bulkupdate() throws Exception {

        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        /* db와 영속성 컨텍스트 데이터가 달라질 경우 차라리 flush / clear로 지워버리는 게 편함 */
        em.flush();
        em.clear();

        //영속성 컨텍스트에 중복 데이터가 있을 때 db 데이터가 아니라 영속성 컨텍스트의 데이터가 우선권을 가짐
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

    }

    /**
     * 수정
     */
    @Test
    public void bulkAdd() throws Exception {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1)) //곱하기 연산 => multiply(2) 빼기의 경우 add(-2)
                .execute();
    }

    /**
     * 삭제
     */
    @Test
    public void bulkDelete() throws Exception {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }


    /**
     * SQL function 호출
     * 만약 내가 추가적으로 생성한 함수(커스텀 함수)를 사용하고 싶은 경우
     * 내가 사용하는 db => 현재 H2 db 사용 중
     * => H2Dialect 상속 받는 class 만들어서 application.yml 에 등록한 뒤 사용해야 함
     */
    @Test
    public void sqlFunction() throws Exception {
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate(
                                "function('replace' ,{0} , {1} , {2} )",
                                member.username,
                                "member", "M"
                        )
                ).from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 소문자로 변경하는 함수
     */
    @Test
    public void sqlFunction2() throws Exception {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(
//                        member.username.eq(Expressions.stringTemplate("function('lower' , {0})", member.username))
//                )
                .where(member.username.eq(member.username.lower()))
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}
