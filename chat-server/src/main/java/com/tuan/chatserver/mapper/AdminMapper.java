package com.tuan.chatserver.mapper;

import com.tuan.chatserver.dto.AdminDTO;
import com.tuan.chatserver.entity.Admin;

public class AdminMapper {
    public static AdminDTO mapAdminToAdminDTO(Admin admin){
        Long id=admin.getId();
        String username=admin.getUsername();
        boolean isActive=admin.isActive();
        return new AdminDTO(id,username,isActive);
    }
}
