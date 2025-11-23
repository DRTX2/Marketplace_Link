package com.gpis.marketplace_link.services.appeal;

import com.gpis.marketplace_link.dto.Messages;
import com.gpis.marketplace_link.dto.appeal.*;
import com.gpis.marketplace_link.dto.incidence.ModeratorInfo;
import com.gpis.marketplace_link.dto.incidence.SimplePublicationResponse;
import com.gpis.marketplace_link.dto.incidence.SimpleReportResponse;
import com.gpis.marketplace_link.dto.incidence.UserSimpleResponse;
import com.gpis.marketplace_link.entities.Appeal;
import com.gpis.marketplace_link.entities.Incidence;
import com.gpis.marketplace_link.entities.Publication;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.enums.AppealDecision;
import com.gpis.marketplace_link.enums.AppealStatus;
import com.gpis.marketplace_link.enums.EmailType;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import com.gpis.marketplace_link.exceptions.business.appeals.AppealNotFoundException;
import com.gpis.marketplace_link.exceptions.business.appeals.UnauthorizedAppealDecisionException;
import com.gpis.marketplace_link.exceptions.business.users.ModeratorNotFoundException;
import com.gpis.marketplace_link.repositories.AppealRepository;
import com.gpis.marketplace_link.repositories.IncidenceRepository;
import com.gpis.marketplace_link.repositories.PublicationRepository;
import com.gpis.marketplace_link.repositories.UserRepository;
import com.gpis.marketplace_link.security.service.SecurityService;
import com.gpis.marketplace_link.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class AppealServiceImp implements AppealService {

    private final SecurityService securityService;
    private final AppealRepository appealRepository;
    private final UserRepository userRepository;
    private final PublicationRepository publicationRepository;
    private final IncidenceRepository incidenceRepository;
    private final NotificationService notificationService;

    @Transactional
    @Override
    public void autoAssignModeratorsToPendingAppeals() {
        // primero buscar todas las pendientes
        List<Appeal> pendingAppeals = appealRepository.findAllPendingWithModerator();

        for (Appeal appeal : pendingAppeals) {
            // para cada apeal pendiente, asignar un nuevo moderador (sin considerar el de la incidencia de ese apppeal)
            Long moderatorIncidence = appeal.getIncidence().getModerator().getId();
            Long newModeratorId = userRepository.findLeastBusyModeratorOrAdminExcludingId(moderatorIncidence);
            log.info("Asignando nuevo moderador con ID {} al appeal con ID {}", newModeratorId, appeal.getId());

            if (newModeratorId != null) {
                User newModUser = userRepository
                        .findById(newModeratorId)
                        .orElseThrow(() -> new ModeratorNotFoundException(Messages.MODERATOR_NOT_FOUND_WITH_ID + newModeratorId));

                appeal.setNewModerator(newModUser);
                appeal.setStatus(AppealStatus.ASSIGNED);
                appealRepository.save(appeal);

                this.sendModeratorAssignedEmailToSeller(appeal);
                this.sendNewAppealAssignedToModerator(appeal);
            }
        }
    }

    @Override
    public Page<AppealSimpleDetailsResponse> fetchAll(Pageable pageable) {
        Long currentUserId = securityService.getCurrentUserId();
        Page<Appeal> appeals = appealRepository.findAppealsByUserId(currentUserId, pageable);

        return appeals.map((appeal -> {
            AppealSimpleDetailsResponse response = new AppealSimpleDetailsResponse();
            response.setId(appeal.getId());
            response.setStatus(appeal.getStatus());
            response.setCreatedAt(appeal.getCreatedAt());
            response.setFinalDecision(appeal.getFinalDecision());

            // datos del nuevo moderador
            ModeratorInfo newModerator = new ModeratorInfo();
            User newModUser = appeal.getNewModerator();
            newModerator.setId(newModUser.getId());
            newModerator.setEmail(newModUser.getEmail());
            newModerator.setFullname(newModUser.getFullName());
            response.setNewModerator(newModerator);

            // datos del vendedor que apelo
            UserSimpleResponse seller = new UserSimpleResponse();
            User sellerUser = appeal.getSeller();
            seller.setId(sellerUser.getId());
            seller.setFullname(sellerUser.getFullName());
            seller.setEmail(sellerUser.getEmail());
            response.setSeller(seller);

            return response;
        }));
    }

    @Override
    public AppealDetailsResponse fetchAppealDetails(Long appealId) {

        // Verificando que el appeal exista y que el usuario actual sea realmente el dueño
        Long currentUserId = securityService.getCurrentUserId();

        Appeal appeal = appealRepository.findAppealByUserId(appealId)
                .orElseThrow(() -> new AppealNotFoundException(Messages.APPEAL_NOT_FOUND + appealId));

        if (!Objects.equals(appeal.getNewModerator().getId(), currentUserId)) {
            throw new UnauthorizedAppealDecisionException(Messages.APPEAL_USER_NOT_AUTHORIZED);
        }

        AppealDetailsResponse response = new AppealDetailsResponse();
        response.setId(appeal.getId());
        response.setStatus(appeal.getStatus());
        response.setCreatedAt(appeal.getCreatedAt());
        response.setFinalDecision(appeal.getFinalDecision());

        // datos del nuevo moderador
        ModeratorInfo newModerator = new ModeratorInfo();
        User newModUser = appeal.getNewModerator();
        newModerator.setId(newModUser.getId());
        newModerator.setEmail(newModUser.getEmail());
        newModerator.setFullname(newModUser.getFullName());
        response.setNewModerator(newModerator);

        // datos del vendedor que apelo
        UserSimpleResponse seller = new UserSimpleResponse();
        User sellerUser = appeal.getSeller();
        seller.setId(sellerUser.getId());
        seller.setFullname(sellerUser.getFullName());
        seller.setEmail(sellerUser.getEmail());
        response.setSeller(seller);

        // datos de la incidencia
        AppealIncidenceResponse incidenceResponse = new AppealIncidenceResponse();
        Incidence incidence = appeal.getIncidence();
        incidenceResponse.setPublicIncidenceUi(incidence.getPublicUi());
        incidenceResponse.setModeratorComment(incidence.getModeratorComment());
        incidenceResponse.setStatus(incidence.getStatus());
        incidenceResponse.setCreatedAt(incidence.getCreatedAt());

        // datos de la incidencia, moderador previo
        ModeratorInfo moderatorInfo = new ModeratorInfo();
        User incidenceModUser = appeal.getIncidence().getModerator();
        moderatorInfo.setId(incidenceModUser.getId());
        moderatorInfo.setEmail(incidenceModUser.getEmail());
        moderatorInfo.setFullname(incidenceModUser.getFullName());
        incidenceResponse.setPreviousModerator(moderatorInfo);

        // datos de la incidenca, publicacion
        SimplePublicationResponse publicationResponse = new SimplePublicationResponse();
        Publication publication = incidence.getPublication();
        publicationResponse.setName(publication.getName());
        publicationResponse.setId(publication.getId());
        publicationResponse.setDescription(publication.getDescription());
        publicationResponse.setStatus(publication.getStatus());
        incidenceResponse.setPublication(publicationResponse);

        // datos de la incidencia, reportes
        List<SimpleReportResponse> reportResponses = incidence.getReports().stream().map((report -> {
            SimpleReportResponse reportResponse = new SimpleReportResponse();
            reportResponse.setId(report.getId());
            reportResponse.setReason(report.getReason());
            reportResponse.setComment(report.getComment());
            reportResponse.setCreatedAt(report.getCreatedAt());

            // el reportador
            UserSimpleResponse reporterResponse = new UserSimpleResponse();
            User reporterUser = report.getReporter();
            reporterResponse.setId(reporterUser.getId());
            reporterResponse.setFullname(reporterUser.getFullName());
            reporterResponse.setEmail(reporterUser.getEmail());

            reportResponse.setReporter(reporterResponse);

            return reportResponse;
        })).toList();

        incidenceResponse.setReports(reportResponses);
        response.setIncidence(incidenceResponse);

        return response;
    }

        @Transactional
        @Override
        public AppealSimpleResponse makeDecision(MakeAppealDecisionRequest req) {
            Appeal appeal = appealRepository.findById(req.getAppealId()).orElseThrow(() -> new AppealNotFoundException(Messages.APPEAL_NOT_FOUND + req.getAppealId()));

            Long currentUserId = securityService.getCurrentUserId();
            boolean isAuthorized = appealRepository.isUserAuthorizedToDecideAppeal(appeal.getId(), currentUserId);
            if (!isAuthorized) {
                throw new UnauthorizedAppealDecisionException(Messages.APPEAL_USER_NOT_AUTHORIZED);
            }

            if (!appeal.getStatus().equals(AppealStatus.ASSIGNED)) {
                throw new UnauthorizedAppealDecisionException(Messages.APPEAL_NOT_ASSIGNED);
            }

            if (!appeal.getFinalDecision().equals(AppealDecision.PENDING) || appeal.getFinalDecisionAt() != null) {
                throw new UnauthorizedAppealDecisionException(Messages.APPEAL_ALREADY_DECIDED);
            }

            if (!appeal.getIncidence().getStatus().equals(IncidenceStatus.APPEALED)) {
                throw new UnauthorizedAppealDecisionException(Messages.APPEAL_INVALID_INCIDENT_STATUS);
            }

            appeal.getIncidence().setStatus(IncidenceStatus.RESOLVED);
            appeal.setStatus(AppealStatus.REVIEWED);
            appeal.setFinalDecision(req.getFinalDecision());
            appeal.setFinalDecisionAt(LocalDateTime.now());
            Publication publication = appeal.getIncidence().getPublication();

            if (req.getFinalDecision().equals(AppealDecision.ACCEPTED)) {
                publication.setVisible();
                this.sendAppealApprovedEmailToSeller(appeal);
            }
            if (req.getFinalDecision().equals(AppealDecision.REJECTED)) {
                publication.setBlocked();
                this.sendAppealRejectedEmailToSeller(appeal);
            }

            AppealSimpleResponse response = new AppealSimpleResponse();
            response.setIncidenceStatus(appeal.getIncidence().getStatus());
            response.setAppealStatus(appeal.getStatus());
            response.setFinalDecision(appeal.getFinalDecision());
            response.setFinalDecisionAt(appeal.getFinalDecisionAt());
            response.setPublicationStatus(publication.getStatus());
            response.setMessage(Messages.APPEAL_DECISION_SUCCESS);

            return response;
        }

    public void sendAppealApprovedEmailToSeller(Appeal appeal) {
            Publication pub = appeal.getIncidence().getPublication();
            User vendor = appeal.getSeller();

            Map<String, String> variables = Map.of(
                    "fullName", vendor.getFullName(),
                    "publicationName", pub.getName()
            );
            notificationService.sendAsync(vendor.getEmail(), EmailType.APPEAL_APPROVED_NOTIFICATION, variables);
        }

    public void sendAppealRejectedEmailToSeller(Appeal appeal) {
            Publication pub = appeal.getIncidence().getPublication();
            User vendor = appeal.getSeller();

            Map<String, String> variables = Map.of(
                    "fullName", vendor.getFullName(),
                    "publicationName", pub.getName()
            );
            notificationService.sendAsync(vendor.getEmail(), EmailType.APPEAL_REJECTED_NOTIFICATION, variables);
        }

    public void sendModeratorAssignedEmailToSeller(Appeal appeal) {
        Publication pub = appeal.getIncidence().getPublication();
        User vendor = appeal.getSeller();
        User newModerator = appeal.getNewModerator();

        Map<String, String> variables = Map.of(
                "fullName", vendor.getFullName(),
                "publicationName", pub.getName(),
                "moderatorName", newModerator.getFullName(),
                "moderatorEmail", newModerator.getEmail()
        );

        notificationService.sendAsync(
                vendor.getEmail(),
                EmailType.APPEAL_SELLER_MODERATOR_ASSIGNED_NOTIFICATION,
                variables
        );
    }

    public void sendNewAppealAssignedToModerator(Appeal appeal) {
        Publication pub = appeal.getIncidence().getPublication();
        User newModerator = appeal.getNewModerator();
        User seller = appeal.getSeller();

        Map<String, String> variables = Map.of(
                "moderatorName", newModerator.getFullName(),
                "sellerName", seller.getFullName(),
                "sellerEmail", seller.getEmail(),
                "publicationName", pub.getName()
        );

        notificationService.sendAsync(
                newModerator.getEmail(),
                EmailType.NEW_APPEAL_ASSIGNED_TO_MODERATOR_NOTIFICATION,
                variables
        );
    }

}
