package study.querydsl.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import study.querydsl.dto.TeamDTO;
import study.querydsl.entity.Team;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TeamMapper {
    TeamMapper INSTANCE = Mappers.getMapper(TeamMapper.class);

    @Mapping(target = "regTime", dateFormat = "yyyy.MM.dd")
    @Mapping(target = "modTime", dateFormat = "yyyy.MM.dd")
    TeamDTO teamToTeamDto(Team Team);
}
