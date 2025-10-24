package com.gpis.marketplace_link.services.user.usecases;

import com.gpis.marketplace_link.entities.Appeal;
import com.gpis.marketplace_link.entities.Incidence;
import com.gpis.marketplace_link.entities.Publication;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.enums.AppealDecision;
import com.gpis.marketplace_link.enums.AppealStatus;
import com.gpis.marketplace_link.enums.EmailType;
import com.gpis.marketplace_link.enums.IncidenceDecision;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import com.gpis.marketplace_link.repositories.AppealRepository;
import com.gpis.marketplace_link.repositories.IncidenceRepository;
import com.gpis.marketplace_link.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReleaseModeratorAssignmentsUseCase {

    private final IncidenceRepository incidenceRepository;
    private final AppealRepository appealRepository;
    private final NotificationService notificationService;

    @Transactional
    public void release(Long moderatorId) {
        releaseIncidences(moderatorId);
        releaseAppeals(moderatorId);
    }

    private void releaseIncidences(Long moderatorId) {
        List<Incidence> incidences = incidenceRepository.findAllByModeratorIdAndStatusIn(
                moderatorId,
                List.of(IncidenceStatus.UNDER_REVIEW)
        );

        if (incidences.isEmpty()) {
            return;
        }

        for (Incidence incidence : incidences) {
            incidence.setModerator(null);
            incidence.setStatus(IncidenceStatus.PENDING_REVIEW);
            incidence.setModeratorComment(null);
            incidence.setDecision(IncidenceDecision.PENDING);
        }

        incidenceRepository.saveAll(incidences);
    }

    private void releaseAppeals(Long moderatorId) {
        List<Appeal> appeals = appealRepository.findAllAssignedByModerator(moderatorId);

        if (appeals.isEmpty()) {
            return;
        }

        for (Appeal appeal : appeals) {
            appeal.setNewModerator(null);
            appeal.setStatus(AppealStatus.FAILED_NO_MOD);
            appeal.setFinalDecision(AppealDecision.PENDING);
            appeal.setFinalDecisionAt(null);
            notifySellerPendingAssignment(appeal);
        }

        appealRepository.saveAll(appeals);
    }

    private void notifySellerPendingAssignment(Appeal appeal) {
        Publication publication = appeal.getIncidence().getPublication();
        User seller = appeal.getSeller();

        Map<String, String> variables = Map.of(
                "fullName", seller.getFullName(),
                "publicationName", publication.getName()
        );

        notificationService.sendAsync(
                seller.getEmail(),
                EmailType.APPEAL_PENDING_ASSIGNMENT_NOTIFICATION,
                variables
        );
    }
}
