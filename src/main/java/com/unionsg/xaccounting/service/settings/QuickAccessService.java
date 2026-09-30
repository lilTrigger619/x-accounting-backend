package com.unionsg.xaccounting.service.settings;

import com.unionsg.xaccounting.dto.settings.AddQuickAccessItemRequest;
import com.unionsg.xaccounting.dto.settings.QuickAccessItemResponse;
import com.unionsg.xaccounting.dto.settings.ReorderQuickAccessRequest;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.settings.QuickAccessItem;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.settings.QuickAccessItemRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuickAccessService {

    private final QuickAccessItemRepository repository;

    private User currentUser() {
        User user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new BusinessException("No authenticated user");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public List<QuickAccessItemResponse> list() {
        UUID userId = currentUser().getId();
        return repository.findByUserIdOrderBySortOrderAsc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public QuickAccessItemResponse add(AddQuickAccessItemRequest request) {
        if (request.getLabel() == null || request.getLabel().isBlank()) {
            throw new BusinessException("Label is required");
        }
        if (request.getPath() == null || request.getPath().isBlank()) {
            throw new BusinessException("Path is required");
        }

        User user = currentUser();
        if (repository.existsByUserIdAndPath(user.getId(), request.getPath())) {
            throw new BusinessException("This menu item is already in your quick access list");
        }

        QuickAccessItem item = new QuickAccessItem();
        item.setUser(user);
        item.setLabel(request.getLabel());
        item.setPath(request.getPath());
        item.setSortOrder((int) repository.countByUserId(user.getId()));

        QuickAccessItem saved = repository.save(item);
        return toResponse(saved);
    }

    @Transactional
    public void remove(Long id) {
        UUID userId = currentUser().getId();
        QuickAccessItem item = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException("Quick access item not found with ID: " + id));
        repository.delete(item);
    }

    @Transactional
    public void reorder(ReorderQuickAccessRequest request) {
        UUID userId = currentUser().getId();
        List<Long> orderedIds = request.getOrderedIds();
        if (orderedIds == null) {
            throw new BusinessException("orderedIds is required");
        }

        for (int i = 0; i < orderedIds.size(); i++) {
            Long itemId = orderedIds.get(i);
            QuickAccessItem item = repository.findByIdAndUserId(itemId, userId)
                    .orElseThrow(() -> new BusinessException(
                            "Quick access item not found with ID: " + itemId));
            item.setSortOrder(i);
            repository.save(item);
        }
    }

    private QuickAccessItemResponse toResponse(QuickAccessItem item) {
        QuickAccessItemResponse response = new QuickAccessItemResponse();
        response.setId(item.getId());
        response.setLabel(item.getLabel());
        response.setPath(item.getPath());
        response.setSortOrder(item.getSortOrder());
        return response;
    }
}
