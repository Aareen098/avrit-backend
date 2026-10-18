package com.hireflow.settings;

import com.hireflow.user.UserDto;
import java.util.List;

// Answer of GET /settings: { company, ai, team }
public record SettingsDto(CompanyDto company, AiDto ai, List<UserDto> team) {
}