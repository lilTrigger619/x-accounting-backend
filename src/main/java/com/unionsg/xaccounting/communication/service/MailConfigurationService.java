package com.unionsg.xaccounting.communication.service;

import com.unionsg.xaccounting.communication.domain.MailConfiguration;
import com.unionsg.xaccounting.communication.dto.MailConfigurationRequest;
import com.unionsg.xaccounting.communication.dto.MailConfigurationResponse;
import com.unionsg.xaccounting.communication.exception.CommunicationException;
import com.unionsg.xaccounting.communication.mapper.CommunicationMapper;
import com.unionsg.xaccounting.communication.repository.MailConfigurationRepository;
import com.unionsg.xaccounting.communication.util.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MailConfigurationService {

    private final MailConfigurationRepository repository;
    private final EncryptionService encryptionService;

    @Transactional(readOnly = true)
    public List<MailConfigurationResponse> listAll() {
        return repository.findAll()
                .stream()
                .map(CommunicationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MailConfigurationResponse getById(Long id) {
        MailConfiguration config = findById(id);
        return CommunicationMapper.toResponse(config);
    }

    @Transactional
    public MailConfigurationResponse create(MailConfigurationRequest request) {
        MailConfiguration config = CommunicationMapper.toEntity(request, encryptionService);
        MailConfiguration saved = repository.save(config);
        return CommunicationMapper.toResponse(saved);
    }

    @Transactional
    public MailConfigurationResponse update(Long id, MailConfigurationRequest request) {
        MailConfiguration config = findById(id);
        CommunicationMapper.applyUpdate(config, request, encryptionService);
        MailConfiguration saved = repository.save(config);
        return CommunicationMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        MailConfiguration config = findById(id);
        repository.delete(config);
    }

    private MailConfiguration findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new CommunicationException("Mail configuration not found with id: " + id));
    }
}

