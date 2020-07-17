package org.openmrs.module.ehrreports.reporting.calculation;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.calculation.result.ListResult;
import org.openmrs.module.ehrreports.metadata.OutpatientMetadata;
import org.openmrs.module.ehrreports.reporting.utils.EhrCalculationUtils;
import org.openmrs.module.ehrreports.reporting.utils.EhrReportConstants;
import org.springframework.stereotype.Component;

@Component
public class PatientOccurenceCalculation extends AbstractPatientCalculation {

  @Override
  public CalculationResultMap evaluate(
      Collection<Integer> cohort,
      Map<String, Object> parameterValues,
      PatientCalculationContext context) {
    EhrCalculationService ehrCalculationService =
        Context.getRegisteredComponents(EhrCalculationService.class).get(0);
    // External dependencies
    OutpatientMetadata outpatientMetadata =
        Context.getRegisteredComponents(OutpatientMetadata.class).get(0);
    EncounterType regReturn = outpatientMetadata.getRegReturnEncounterType();
    EncounterType adultReturn = outpatientMetadata.getAdultReturnEncounterType();
    EncounterType pedReturn = outpatientMetadata.getPedsReturnEncounterType();
    CalculationResultMap resultMap = new CalculationResultMap();
    EhrReportConstants.OccurenceStates state =
        (EhrReportConstants.OccurenceStates) parameterValues.get("state");

    CalculationResultMap allEncounters =
        ehrCalculationService.allEncounters(
            Arrays.asList(regReturn, adultReturn, pedReturn),
            cohort,
            null,
            context.getNow(),
            context);
    CalculationResultMap getObs =
        ehrCalculationService.lastObs(
            outpatientMetadata.getRegistrationFeeConcept(), cohort, context);
    for (Integer pId : cohort) {
      boolean isCandidate = false;
      ListResult pregnantResult = (ListResult) allEncounters.get(pId);
      List<Encounter> encounterList = EhrCalculationUtils.extractResultValues(pregnantResult);
      Obs obs = EhrCalculationUtils.resultForPatient(getObs, pId);

      if ((encounterList.size() > 0
          && (state.equals(EhrReportConstants.OccurenceStates.REVISIT)
              && (obs != null
                  && obs.getValueCoded() != null
                  && obs.getValueCoded().equals(outpatientMetadata.getRevisitConcept()))))) {
        isCandidate = true;

      } else if ((encounterList.size() > 0
          && (state.equals(EhrReportConstants.OccurenceStates.NEW))
          && (obs != null
              && obs.getValueCoded() != null
              && obs.getValueCoded().equals(outpatientMetadata.getNewPatientConcept())))) {
        isCandidate = true;
      }
      resultMap.put(pId, new BooleanResult(isCandidate, this));
    }
    return resultMap;
  }
}
