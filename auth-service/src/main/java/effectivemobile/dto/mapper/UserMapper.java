package effectivemobile.dto.mapper;

import effectivemobile.dto.UserDto;
import effectivemobile.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
}
