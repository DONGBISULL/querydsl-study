package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.transaction.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
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

    @Test
    public void startJPQL() {
        //member1을 찾아라
        String qlString = "select m from Member m where m.username = :username"; // 메서드가 실행됐을 때 에러 발생
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {

//        QMember m1 = new QMember("m1"); // 같은 테이블 조인할 경우에만 선언하여 사용
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.between(10, 20)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void sendAndParam() {
        // null 을 무시하므로 동적 쿼리 만들때 활용
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10),
                        null
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();

        QueryResults<Member> fetchResults = queryFactory
                .selectFrom(member)
                .fetchResults(); // 사용 지양

        fetchResults.getTotal();
        List<Member> content = fetchResults.getResults();

        long total = queryFactory
                .selectFrom(member)
                .fetchCount(); // 사용지양

    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순 (desc)
     * 2. 회원 이름 올림차순 (asc)
     * 단 2에서 회원이름이 없을 경우 마지막 출력(null last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(
                        member.age.desc(),
                        member.username.asc().nullsLast()
                )
                .fetch();
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);

    }

    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); //10 + 10 + 10

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() throws Exception {
        List<Member> result = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인
     * 회원 이름이 팀 이름과 같은 회원 조인
     */
    @Test
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("uesrname")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서,팀 이름의 teamA 인 팀만 조인 , 회원은 모두 조회
     * JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.team, team)
                .from(member)
//                .join(member.team , team)
//                .where(team.name.eq("teamA"))
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 연관 관계가 없는 엔티티 외부 조인
     * 회원 이름이 팀 이름과 같은 회원 조인을 외부 조인
     */
    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
//                 .leftJoin(member.team, team) // id를 통해 자동으로 매칭됨
                .leftJoin(team).on(member.username.eq(team.name)) // 막조인의 경우 left 조인에 한 번에 넣는 것이 아니라 분리하여 입력
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf; // EntityManager 인스턴스를 관리

    /**
     * fetch join
     * => 성능 최적화에서 주로 사용
     */
    @Test
    public void fetchJoinNo() throws Exception {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isFalse();
    }

    /**
     * fetch join 과 달리 연관 entity에  join을 걸어도 실제 쿼리에서 조회하는 엔티티는
     * JPQL에서 조회하는 주체가 되는 Entity만 조회하여 영속화
     * <p>
     * fetch join
     * 조회의 주체가 되는 Entity 이외에 fetch join 이 걸린 연관 entity도 함께 select 하여 모두 영속화
     * => 즉시 로딩
     * <p>
     * 1) fetch join에서는 별칭 사용 X
     * => 최대한 지양해라!!
     * <p>
     * 2) 둘 이상의 컬렉션 페치 조인 X
     * => 데이터 정합성이 맞지 않는 경우 생김
     * <p>
     * 3) 컬렉션을 페치 조인하면 페이징 API 사용X
     */
    @Test
    public void fetchJoinUse() throws Exception {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isTrue();
    }

    /**
     * ExpressionUtils.as() as를 통해 서브쿼리의 결과물을 alias 로 !!
     * Where 절에서 서브 쿼리를 사용할 때 ExpressionUtils 를 사용하여 구성한다!!
     * */

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery_test() throws Exception {

        QMember memberSub = new QMember("memberSub"); // 알리아스 중복을 없애기 위해!!!

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                )).fetch();

        assertThat(result).extracting("age").containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원
     */
    @Test
    public void subQuery_age_avg_goe_test() throws Exception {

        QMember memberSub = new QMember("memberSub"); // 알리아스 중복을 없애기 위해!!!

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                )).fetch();

        assertThat(result).extracting("age").containsExactly(30, 40);

    }

    /**
     * 서브쿼리 여러 건 처리, in 사용
     */
    @Test
    public void subQueryIn() throws Exception {

        QMember memberSub = new QMember("memberSub"); // 알리아스 중복을 없애기 위해!!!

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(20, 30, 40);

    }

    /**
     * 이름과 함께 서브쿼리 사용하기
     */
    @Test
    public void selectSubquery() {

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(member.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple " + tuple);
        }
        /**
         * JPA 서브쿼리 한계 => from 절의 서브쿼리 지원X
         * => 인라인뷰 지원 안한다!!
         * 1) 서브쿼리 join으로 변경
         * 2) 애플리케이션 쿼리 2번 분리하여 실행
         * 3) nativeSQL 사용
         *
         *  from 절에 서브쿼리 쓰는 이유
         * -> 실제 기능의 필요 보다 쿼리에서 다 형식을 맞추려고 쿼리를 짜는 것이므로 쿼리는 단순하게 짜고
         *  날짜 형식 같은 것들은 뷰에서 만들어서 보여주는 것이 좋다 !!!
         *
         *  책 > SQL AntiPatterns Good!!!!
         *  => 정말 복잡한 몇 천줄의 쿼리는 쪼개서 여러번에 걸쳐 조회하는 것이 더 나을 수 있다.
         *
         * */
    }

    /**
     * case 문 구현
     */
    @Test
    public void basicCase() throws Exception {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 복잡한 case문의 경우 CaseBuilder 사용
     * => db에선 최소한의 grouping과 filtering 통해 raw 데이터를 조회
     * 실제 case 별 상황 화면에 보여주기 등은 애플리케이션에서 로직을 수행하는 것이 좋다!!
     */
    @Test
    public void complexCase() throws Exception {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println(" s = " + s);
        }
    }

    /**
     *
     */
    @Test
    public void constant() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    /**
     * concat 사용할 때 타입을 맞춰줘야 한다
     * => stringValue 로 age 의 타입 맞춰줌
     *  주로 enum 처리 할 때 stringValue 로 맞춰서 concat 사용해라 !!
     *
     */
    @Test
    public void concat() throws Exception {
        List<String> result = queryFactory
                .select(member.username.concat(("_")).concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

}
