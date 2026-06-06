package com.miguel.app.system.controller;

import com.miguel.app.system.dto.request.CreateStaffUserRequest;
import com.miguel.app.system.dto.response.ApiResponse;
import com.miguel.app.system.dto.response.PagedResponse;
import com.miguel.app.system.dto.response.UserResponse;
import com.miguel.app.system.service.interfaces.UserAdminService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "5 — ADMIN — Management", description = "Full system administration: users, customers, meters, tariffs, bill generation.")
public class AdminUserController {

    private final UserAdminService userAdminService;

    @PostMapping("/staff")
    @Operation(summary = "Create a staff user")
    public ResponseEntity<ApiResponse<UserResponse>> createStaff(@Valid @RequestBody CreateStaffUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("Staff user created successfully")
                .data(userAdminService.createStaff(request))
                .build());
    }

    @GetMapping("/all")
    @Operation(summary = "List all users in the system including whether they have a linked customer profile")
    public ResponseEntity<PagedResponse<UserResponse>> getAllUsers(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(userAdminService.getAllUsers(pageable));
    }

    @GetMapping
    @Operation(summary = "List staff users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getStaffUsers() {
        return ResponseEntity.ok(ApiResponse.<List<UserResponse>>builder()
                .success(true)
                .message("Staff users retrieved successfully")
                .data(userAdminService.getAllStaffUsers())
                .build());
    }

    @GetMapping("/customers")
    @Hidden
    @Operation(summary = "List customer-role users, e.g. to find a userId for linking a customer profile")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getCustomerUsers(
            @RequestParam(name = "unlinkedOnly", defaultValue = "false") boolean unlinkedOnly,
            @RequestParam(name = "search", required = false) String search) {
        return ResponseEntity.ok(ApiResponse.<List<UserResponse>>builder()
                .success(true)
                .message("Customer users retrieved successfully")
                .data(userAdminService.getCustomerUsers(unlinkedOnly, search))
                .build());
    }

    @GetMapping("/customers/unlinked")
    @Operation(
            summary = "List customer-role users that do not yet have a linked customer profile",
            tags = "03. Admin - Customers"
    )
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUnlinkedCustomerUsers(
            @RequestParam(name = "search", required = false) String search
    ) {
        return ResponseEntity.ok(ApiResponse.<List<UserResponse>>builder()
                .success(true)
                .message("Unlinked customer users retrieved successfully")
                .data(userAdminService.getUnlinkedCustomerUsers(search))
                .build());
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a staff user")
    public ResponseEntity<ApiResponse<UserResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User activated successfully")
                .data(userAdminService.activate(id))
                .build());
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a staff user")
    public ResponseEntity<ApiResponse<UserResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("User deactivated successfully")
                .data(userAdminService.deactivate(id))
                .build());
    }
}
