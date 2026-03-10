package com.pcmanager.infrastructure.persistence.file;

import com.pcmanager.common.exception.BusinessException;
import com.pcmanager.domain.member.Member;
import com.pcmanager.domain.member.UserRole;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MemberFileStore {
    private final Path filePath;

    public MemberFileStore(Path filePath) {
        this.filePath = filePath;
    }

    public List<Member> loadMembers(List<Member> defaults) {
        if (!Files.exists(filePath)) {
            saveMembers(defaults);
            return new ArrayList<>(defaults);
        }
        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            List<Member> members = new ArrayList<>();
            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }
                String[] tokens = line.split("\\|", -1);
                if (tokens.length != 9) {
                    throw new BusinessException("회원 파일 형식이 올바르지 않습니다.");
                }
                members.add(new Member(
                        Long.parseLong(tokens[0]),
                        tokens[1],
                        tokens[2],
                        tokens[3],
                        tokens[4],
                        Integer.parseInt(tokens[5]),
                        Integer.parseInt(tokens[6]),
                        tokens[7],
                        UserRole.valueOf(tokens[8])
                ));
            }
            return members;
        } catch (IOException exception) {
            throw new BusinessException("회원 파일을 읽지 못했습니다. " + exception.getMessage());
        }
    }

    public void saveMembers(List<Member> members) {
        try {
            Files.createDirectories(filePath.getParent());
            List<String> lines = members.stream()
                    .map(member -> String.join("|",
                            String.valueOf(member.getMemberId()),
                            member.getLoginId(),
                            member.getPassword(),
                            member.getName(),
                            member.getPhone(),
                            String.valueOf(member.getRemainingMinutes()),
                            String.valueOf(member.getPoint()),
                            member.getGrade(),
                            member.getUserRole().name()))
                    .toList();
            Files.write(filePath, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BusinessException("회원 파일을 저장하지 못했습니다. " + exception.getMessage());
        }
    }
}
