package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.payroll.ChangeCompensationRequest;
import com.unionsg.xaccounting.dto.payroll.ChangeEmployeeStatusRequest;
import com.unionsg.xaccounting.dto.payroll.CreateEmployeeRequest;
import com.unionsg.xaccounting.dto.payroll.EmployeeCompensationResponse;
import com.unionsg.xaccounting.dto.payroll.EmployeeResponse;
import com.unionsg.xaccounting.dto.payroll.UpdateEmployeeRequest;
import com.unionsg.xaccounting.entity.payroll.Employee;
import com.unionsg.xaccounting.entity.payroll.EmployeeSalaryStructure;
import com.unionsg.xaccounting.entity.payroll.SalaryStructure;
import com.unionsg.xaccounting.enums.EmploymentStatus;
import com.unionsg.xaccounting.enums.EntityType;
import com.unionsg.xaccounting.enums.PayrollAuditAction;
import com.unionsg.xaccounting.enums.PayrollAuditEntityType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.dto.FileUploadRequestDto;
import com.unionsg.xaccounting.repository.payroll.EmployeeRepository;
import com.unionsg.xaccounting.repository.payroll.EmployeeSalaryStructureRepository;
import com.unionsg.xaccounting.service.DocumentNumberService;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import com.unionsg.xaccounting.service.FileService.FileService;
import com.unionsg.xaccounting.enums.DocumentModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The employee payroll master (§2). Employment-status transitions and compensation changes are
 * the two things this service treats with real care: a terminated employee must not silently
 * reappear in a future payroll run (§2), and a compensation change must never edit history - it
 * closes the previous {@link EmployeeSalaryStructure} assignment and opens a new one (§6).
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeSalaryStructureRepository employeeSalaryStructureRepository;
    private final DepartmentService departmentService;
    private final PositionService positionService;
    private final WorkLocationService workLocationService;
    private final PayrollGroupService payrollGroupService;
    private final SalaryStructureService salaryStructureService;
    private final DocumentNumberService documentNumberGeneratorService;
    private final PayrollAuditLogService auditLogService;
    private final FileService fileService;

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        Employee employee = new Employee();
        employee.setEmployeeNumber(documentNumberGeneratorService.generateNextNumber(DocumentModule.EMPLOYEE));
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setWorkEmail(request.getWorkEmail());
        employee.setPersonalEmail(request.getPersonalEmail());
        employee.setPhone(request.getPhone());
        employee.setDateOfBirth(request.getDateOfBirth());
        employee.setNationalId(request.getNationalId());
        if (request.getDepartmentId() != null) {
            employee.setDepartment(departmentService.getEntity(request.getDepartmentId()));
        }
        if (request.getPositionId() != null) {
            employee.setPosition(positionService.getEntity(request.getPositionId()));
        }
        if (request.getWorkLocationId() != null) {
            employee.setWorkLocation(workLocationService.getEntity(request.getWorkLocationId()));
        }
        if (request.getEmploymentType() != null) {
            employee.setEmploymentType(request.getEmploymentType());
        }
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
        employee.setDateOfEmployment(request.getDateOfEmployment());
        employee.setPayrollGroup(payrollGroupService.getEntity(request.getPayrollGroupId()));
        employee.setBankName(request.getBankName());
        employee.setBankAccountName(request.getBankAccountName());
        employee.setBankAccountNumber(request.getBankAccountNumber());
        employee.setBankRoutingNumber(request.getBankRoutingNumber());
        if (request.getCurrency() != null) {
            employee.setCurrency(request.getCurrency());
        }
        employee.setTaxIdentifier(request.getTaxIdentifier());
        employee.setStatutoryId(request.getStatutoryId());
        employee.setNotes(request.getNotes());

        Employee saved = employeeRepository.save(employee);

        if (request.getSalaryStructureId() != null && request.getBasicSalary() != null) {
            SalaryStructure structure = salaryStructureService.getEntity(request.getSalaryStructureId());
            EmployeeSalaryStructure assignment = new EmployeeSalaryStructure();
            assignment.setEmployee(saved);
            assignment.setSalaryStructure(structure);
            assignment.setBasicSalary(request.getBasicSalary());
            assignment.setEffectiveFrom(
                    request.getCompensationEffectiveFrom() != null
                            ? request.getCompensationEffectiveFrom()
                            : request.getDateOfEmployment() != null ? request.getDateOfEmployment() : LocalDate.now());
            assignment.setCurrent(true);
            assignment.setReason("Initial compensation on hire");
            employeeSalaryStructureRepository.save(assignment);
        }

        auditLogService.record(PayrollAuditEntityType.EMPLOYEE, saved.getId(), PayrollAuditAction.CREATED,
                null, saved.getEmployeeNumber() + " - " + saved.getFullName(), null);

        return toResponse(saved);
    }

    @Transactional
    public EmployeeResponse update(Long id, UpdateEmployeeRequest request) {
        Employee employee = getEntity(id);
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setWorkEmail(request.getWorkEmail());
        employee.setPersonalEmail(request.getPersonalEmail());
        employee.setPhone(request.getPhone());
        employee.setDateOfBirth(request.getDateOfBirth());
        employee.setNationalId(request.getNationalId());
        if (request.getDepartmentId() != null) {
            employee.setDepartment(departmentService.getEntity(request.getDepartmentId()));
        }
        if (request.getPositionId() != null) {
            employee.setPosition(positionService.getEntity(request.getPositionId()));
        }
        if (request.getWorkLocationId() != null) {
            employee.setWorkLocation(workLocationService.getEntity(request.getWorkLocationId()));
        }
        if (request.getEmploymentType() != null) {
            employee.setEmploymentType(request.getEmploymentType());
        }
        if (request.getPayrollGroupId() != null) {
            employee.setPayrollGroup(payrollGroupService.getEntity(request.getPayrollGroupId()));
        }
        employee.setBankName(request.getBankName());
        employee.setBankAccountName(request.getBankAccountName());
        employee.setBankAccountNumber(request.getBankAccountNumber());
        employee.setBankRoutingNumber(request.getBankRoutingNumber());
        if (request.getCurrency() != null) {
            employee.setCurrency(request.getCurrency());
        }
        employee.setTaxIdentifier(request.getTaxIdentifier());
        employee.setStatutoryId(request.getStatutoryId());
        employee.setNotes(request.getNotes());

        auditLogService.record(PayrollAuditEntityType.EMPLOYEE, employee.getId(), PayrollAuditAction.UPDATED,
                null, null, null);

        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse changeStatus(Long id, ChangeEmployeeStatusRequest request) {
        Employee employee = getEntity(id);
        EmploymentStatus previous = employee.getEmploymentStatus();
        employee.setEmploymentStatus(request.getStatus());
        if (request.getStatus() == EmploymentStatus.TERMINATED) {
            employee.setTerminationDate(
                    request.getTerminationDate() != null ? request.getTerminationDate() : LocalDate.now());
        }
        Employee saved = employeeRepository.save(employee);

        auditLogService.record(PayrollAuditEntityType.EMPLOYEE, employee.getId(), PayrollAuditAction.STATUS_CHANGED,
                previous.name(), request.getStatus().name(), request.getReason());

        return toResponse(saved);
    }

    /**
     * Assigns a new Salary Structure/basic salary effective from a date, closing off the
     * previous assignment rather than editing it (§6). Rejected if it would overlap the current
     * assignment's start date, since two "current" assignments would make historical resolution
     * ambiguous.
     */
    @Transactional
    public EmployeeCompensationResponse changeCompensation(Long employeeId, ChangeCompensationRequest request) {
        Employee employee = getEntity(employeeId);
        SalaryStructure structure = salaryStructureService.getEntity(request.getSalaryStructureId());

        Optional<EmployeeSalaryStructure> currentOpt =
                employeeSalaryStructureRepository.findByEmployeeIdAndCurrentTrue(employeeId);

        if (currentOpt.isPresent() && !request.getEffectiveFrom().isAfter(currentOpt.get().getEffectiveFrom())) {
            throw new BusinessException(
                    "New compensation must be effective after the current assignment's start date ("
                            + currentOpt.get().getEffectiveFrom() + ")");
        }

        currentOpt.ifPresent(current -> {
            current.setCurrent(false);
            current.setEffectiveTo(request.getEffectiveFrom().minusDays(1));
            employeeSalaryStructureRepository.save(current);
        });

        EmployeeSalaryStructure assignment = new EmployeeSalaryStructure();
        assignment.setEmployee(employee);
        assignment.setSalaryStructure(structure);
        assignment.setBasicSalary(request.getBasicSalary());
        assignment.setEffectiveFrom(request.getEffectiveFrom());
        assignment.setCurrent(true);
        assignment.setReason(request.getReason());
        EmployeeSalaryStructure saved = employeeSalaryStructureRepository.save(assignment);

        auditLogService.record(PayrollAuditEntityType.EMPLOYEE, employeeId, PayrollAuditAction.UPDATED,
                currentOpt.map(c -> c.getBasicSalary().toString()).orElse(null),
                request.getBasicSalary().toString(), request.getReason());

        return toCompensationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EmployeeCompensationResponse> getCompensationHistory(Long employeeId) {
        return employeeSalaryStructureRepository.findByEmployeeIdOrderByEffectiveFromDesc(employeeId).stream()
                .map(this::toCompensationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAll() {
        return employeeRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Employee getEntity(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    /** Employees eligible for a new payroll run in this group - excludes anyone not ACTIVE (§2, §36). */
    @Transactional(readOnly = true)
    public List<Employee> getActiveEmployeesForGroup(Long payrollGroupId) {
        return employeeRepository.findByPayrollGroupIdAndEmploymentStatus(payrollGroupId, EmploymentStatus.ACTIVE);
    }

    public EmployeeResponse toResponse(Employee employee) {
        Optional<EmployeeSalaryStructure> current =
                employeeSalaryStructureRepository.findByEmployeeIdAndCurrentTrue(employee.getId());

        return EmployeeResponse.builder()
                .id(employee.getId())
                .employeeNumber(employee.getEmployeeNumber())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .fullName(employee.getFullName())
                .workEmail(employee.getWorkEmail())
                .personalEmail(employee.getPersonalEmail())
                .phone(employee.getPhone())
                .dateOfBirth(employee.getDateOfBirth())
                .nationalId(employee.getNationalId())
                .departmentId(employee.getDepartment() != null ? employee.getDepartment().getId() : null)
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .positionId(employee.getPosition() != null ? employee.getPosition().getId() : null)
                .positionTitle(employee.getPosition() != null ? employee.getPosition().getTitle() : null)
                .workLocationId(employee.getWorkLocation() != null ? employee.getWorkLocation().getId() : null)
                .workLocationName(employee.getWorkLocation() != null ? employee.getWorkLocation().getName() : null)
                .employmentType(employee.getEmploymentType())
                .employmentStatus(employee.getEmploymentStatus())
                .dateOfEmployment(employee.getDateOfEmployment())
                .terminationDate(employee.getTerminationDate())
                .payrollGroupId(employee.getPayrollGroup() != null ? employee.getPayrollGroup().getId() : null)
                .payrollGroupName(employee.getPayrollGroup() != null ? employee.getPayrollGroup().getName() : null)
                .bankName(employee.getBankName())
                .bankAccountName(employee.getBankAccountName())
                .bankAccountNumber(employee.getBankAccountNumber())
                .bankRoutingNumber(employee.getBankRoutingNumber())
                .currency(employee.getCurrency())
                .taxIdentifier(employee.getTaxIdentifier())
                .statutoryId(employee.getStatutoryId())
                .notes(employee.getNotes())
                .currentBasicSalary(current.map(EmployeeSalaryStructure::getBasicSalary).orElse(null))
                .currentSalaryStructureName(current.map(c -> c.getSalaryStructure().getName()).orElse(null))
                .photoFileId(employee.getPhotoFileId())
                .photoUrl(employee.getPhotoFileId() != null
                        ? "/api/files/" + employee.getPhotoFileId() + "/download"
                        : null)
                .build();
    }

    @Transactional
    public EmployeeResponse uploadPhoto(Long id, MultipartFile photo) {
        Employee employee = getEntity(id);
        if (employee.getPhotoFileId() != null) {
            fileService.deleteFile(employee.getPhotoFileId());
        }
        FileUploadRequestDto uploadRequest = new FileUploadRequestDto();
        uploadRequest.setEntityType(EntityType.EMPLOYEE);
        uploadRequest.setEntityId(employee.getId().toString());
        uploadRequest.setDescription("Employee Photo");
        UUID currentUserId = SecurityUtils.getCurrentUser().getId();
        uploadRequest.setUploadedBy(currentUserId);
        String photoFileId = fileService.uploadFile(new MultipartFile[]{photo}, uploadRequest)
                .get(0)
                .getId();
        employee.setPhotoFileId(photoFileId);
        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse deletePhoto(Long id) {
        Employee employee = getEntity(id);
        if (employee.getPhotoFileId() != null) {
            fileService.deleteFile(employee.getPhotoFileId());
            employee.setPhotoFileId(null);
        }
        return toResponse(employeeRepository.save(employee));
    }

    private EmployeeCompensationResponse toCompensationResponse(EmployeeSalaryStructure assignment) {
        return EmployeeCompensationResponse.builder()
                .id(assignment.getId())
                .salaryStructureId(assignment.getSalaryStructure().getId())
                .salaryStructureName(assignment.getSalaryStructure().getName())
                .basicSalary(assignment.getBasicSalary())
                .effectiveFrom(assignment.getEffectiveFrom())
                .effectiveTo(assignment.getEffectiveTo())
                .current(assignment.isCurrent())
                .reason(assignment.getReason())
                .build();
    }
}
