package com.my.expenditure.service.expenditure;

import com.my.expenditure.entity.Expenditure;
import com.my.expenditure.entity.Tag;
import com.my.expenditure.entity.User;
import com.my.expenditure.repository.ExpenditureRepository;
import com.my.expenditure.repository.TagRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class ExpenditureStatsService {

    @Autowired
    private ExpenditureRepository expenditureRepository;

    @Autowired
    private TagRepository tagRepository;

    public JSONObject getStats(User user, String date) {
        JSONObject jsonObject = new JSONObject()
                .put("date", date)
                .put("currentYear", getCurrentYear(user, date))
                .put("monthTotal", getCurrentMonthTotal(user, date))
                .put("yearTotal", getCurrentYearTotal(user, date))
                .put("previousYearTotal", getPreviousYearTotal(user, date))
                .put("previousMonthTotal", getPreviousMonthTotal(user, date))
                .put("timeSeries", getCurrentYearMonthTotals(user, date))
                .put("topTags", getCurrentMonthTopTags(user, date))
                .put("totalTimeSeries", getTotalTimeSeries(user))
                .put("tagTotals", getTagTotalsThisMonth(user, date))
                .put("previousTagTotals", getTagTotalsLastMonth(user, date));

        return jsonObject;
    }

    private Integer getCurrentYear(User user, String date) {
        return LocalDate.parse(date).getYear();
    }


    private Double getCurrentMonthTotal(User user, String date) {
        Double currentMonthTotal = expenditureRepository.getCurrentMonthTotal(user, date);
        return currentMonthTotal;
    }

    private Double getPreviousMonthTotal(User user, String date) {
        Double previousMonthTotal = expenditureRepository.getPreviousMonthTotal(user, date);

        previousMonthTotal = previousMonthTotal == null ? 0.0 : previousMonthTotal;

        return previousMonthTotal;
    }

    private Double getCurrentYearTotal(User user, String date) {
        Double currentYearTotal = expenditureRepository.getCurrentYearTotal(user, date);
        return currentYearTotal;
    }

    private Double getPreviousYearTotal(User user, String date) {
        Double previousYearTotal = expenditureRepository.getPreviousMonthTotal(user, date);
        return previousYearTotal;
    }

    private List<Double> getCurrentYearMonthTotals(User user, String date) {
        List<Expenditure> expenditures = expenditureRepository.getCurrentYearMonthlyExpenditures(user, date);

        Map<String, Double> timeSeries = new LinkedHashMap<>();

        for (Integer i = 1; i <= 12; i++) {
            timeSeries.put(String.valueOf(i), 0.0);
        }

        Double monthExpenditure = 0.0;
        String month;

        for (Expenditure expenditure : expenditures) {
            month = String.valueOf(LocalDate.parse(expenditure.getDate()).getMonthValue());
            monthExpenditure = timeSeries.get(month);
            timeSeries.put(month, monthExpenditure + expenditure.getAmount());
        }

        return new ArrayList<>(timeSeries.values());
    }

    private JSONArray getCurrentMonthTopTags(User user, String date) {
        Map<Tag, Double> topCurrentMonthTagsTotals = new LinkedHashMap<>();
        Map<Tag, Integer> topCurrentMonthTagsCounts = new LinkedHashMap<>();

        List<Expenditure> expenditures = expenditureRepository.findAllByUserAndMonth(user, date);

        Double unassignedTotal = 0.0;
        Integer unassignedCount = 0;

        for (Expenditure expenditure : expenditures) {
            if (expenditure.getTags().isEmpty()) {
                unassignedTotal += expenditure.getAmount();
                unassignedCount++;
            }
            for (Tag tag : expenditure.getTags()) {
                if (topCurrentMonthTagsTotals.containsKey(tag)) {
                    topCurrentMonthTagsTotals.put(tag, topCurrentMonthTagsTotals.get(tag) + expenditure.getAmount());
                    topCurrentMonthTagsCounts.put(tag, topCurrentMonthTagsCounts.get(tag) + 1);
                } else {
                    topCurrentMonthTagsTotals.put(tag, expenditure.getAmount());
                    topCurrentMonthTagsCounts.put(tag, 1);
                }
            }
        }

        JSONArray jsonArray = new JSONArray();

        for (Tag tag : topCurrentMonthTagsTotals.keySet()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", tag.getId());
            jsonObject.put("name", tag.getName());
            jsonObject.put("monthTotal", topCurrentMonthTagsTotals.get(tag));
            jsonObject.put("monthCount", topCurrentMonthTagsCounts.get(tag));
            jsonArray.put(jsonObject);
        }

        if (unassignedTotal > 0.0) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", -1);
            jsonObject.put("name", "Unassigned");
            jsonObject.put("monthTotal", unassignedTotal);
            jsonObject.put("monthCount", unassignedCount);
            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }

    private Map<String, Double> getTotalTimeSeries(User user) {
        List<Expenditure> expenditures = expenditureRepository.findAllByUser(user);
        Map<String, Double> totalTimeSeries = new HashMap<>();

        for (Expenditure expenditure : expenditures) {
            if (totalTimeSeries.containsKey(expenditure.getDate())) {
                totalTimeSeries.put(expenditure.getDate(), totalTimeSeries.get(expenditure.getDate()) + expenditure.getAmount());
            } else {
                totalTimeSeries.put(expenditure.getDate(), expenditure.getAmount());
            }
        }
        return totalTimeSeries;
    }

    private Map<String, Double> getTagTotalsThisMonth(User user, String date) {

        Map<String, Double> tagTotals = new HashMap<>();
        List<Tag> tags = tagRepository.findAllByUser(user);

        for (Tag tag : tags) {

            if (!tagTotals.containsKey(tag.getName())) {
                tagTotals.put(tag.getName(), 0.0);
            }

            List<Expenditure> expenditures = expenditureRepository.findAllByTagAndDateAndUser(tag, date, user);
            for (Expenditure expenditure : expenditures) {
                tagTotals.put(tag.getName(), tagTotals.get(tag.getName()) + expenditure.getAmount());
            }

            Double currentMonthTotal=expenditureRepository.getCurrentMonthTotal(user,date);

            if(currentMonthTotal!=null){
                tagTotals.put(tag.getName(), Math.round(tagTotals.get(tag.getName()) / currentMonthTotal * 100.0) / 100.0);
            }

        }

        return tagTotals;
    }


    private Map<String, Double> getTagTotalsLastMonth(User user, String date) {
        Map<String, Double> tagTotals = new HashMap<>();
        List<Tag> tags = tagRepository.findAllByUser(user);

        for (Tag tag : tags) {

            if (!tagTotals.containsKey(tag.getName())) {
                tagTotals.put(tag.getName(), 0.0);
            }

            List<Expenditure> expenditures = expenditureRepository.findAllByTagAndDatePreviousAndUser(tag, date, user);
            for (Expenditure expenditure : expenditures) {
                tagTotals.put(tag.getName(), tagTotals.get(tag.getName()) + expenditure.getAmount());
            }

            Double previousMonthTotal=expenditureRepository.getPreviousMonthTotal(user,date);

            if(previousMonthTotal!=null){
                tagTotals.put(tag.getName(), Math.round(tagTotals.get(tag.getName()) / previousMonthTotal * 100.0) / 100.0);
            }
        }

        return tagTotals;
    }

}
