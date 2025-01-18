package com.debuggeandoideas.report_ms.services;

import com.debuggeandoideas.report_ms.helpers.ReportHelper;
import com.debuggeandoideas.report_ms.models.Company;
import com.debuggeandoideas.report_ms.models.WebSite;
import com.debuggeandoideas.report_ms.repositories.CompaniesRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService{
    private final CompaniesRepository companiesRepository;
    private final ReportHelper reportHelper;


    @Override
    public String makeReport(String name) {
        return reportHelper.readTemplate(this.companiesRepository.getByName(name).orElseThrow());

    }

    @Override
    public String saveReport(String report) {
        var format = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        var placeHolders = this.reportHelper.getPlaceHoldersFromTemplate(report);
        var webSites = Stream.of(placeHolders.get(3))
                .map(webSite -> WebSite.builder().name(webSite).build())
                .toList();
        var company = Company.builder()
                .name(placeHolders.get(0))
                .foundationDate(LocalDate.parse(placeHolders.get(1), format))
                .founder(placeHolders.get(2))
                .webSites(webSites)
                .build();
        this.companiesRepository.postByName(company);
        return "Saved";
    }

    @Override
    public void deleteReport(String name) {
        this.companiesRepository.deleteByName(name);
    }
}
