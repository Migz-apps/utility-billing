package com.miguel.app.system.service.interfaces;

import com.miguel.app.system.dto.request.CreateStaffUserRequest;
import com.miguel.app.system.dto.response.PagedResponse;
import com.miguel.app.system.dto.response.UserResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface UserAdminService {
    UserResponse createStaff(CreateStaffUserRequest request);
    PagedResponse<UserResponse> getAllUsers(Pageable pageable);
    List<UserResponse> getAllStaffUsers();
    List<UserResponse> getCustomerUsers(boolean unlinkedOnly, String search);
    List<UserResponse> getUnlinkedCustomerUsers(String search);
    UserResponse activate(Long id);
    UserResponse deactivate(Long id);
}
