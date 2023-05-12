package study.querydsl.repository;

import org.springframework.stereotype.Repository;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Repository // 1.컴포넌트 스캔 + 2. JPA의 예외를 Spring 에서 처리할 수 있도록 함
public class MemberJpaRepository {

    @PersistenceContext
    private EntityManager em;

    public Member save(Member member) {
        em.persist(member);
        return member;
    }

    public void delete(Member member) {
        em.remove(member);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public Optional<Member> findById(Long id) {
        Member member = em.find(Member.class, id);
        return Optional.ofNullable(member);
    }

    public Member find(Long id) {
        return em.find(Member.class, id);
    }

    public long count() {
        return em.createQuery("select count(m) from Member m ", Long.class)
                .getSingleResult();
    }

}
