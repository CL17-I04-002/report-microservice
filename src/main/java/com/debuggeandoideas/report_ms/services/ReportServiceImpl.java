package com.debuggeandoideas.report_ms.services;

import com.debuggeandoideas.report_ms.helpers.ReportHelper;
import com.debuggeandoideas.report_ms.models.Company;
import com.debuggeandoideas.report_ms.models.WebSite;
import com.debuggeandoideas.report_ms.repositories.CompaniesFallbackRepository;
import com.debuggeandoideas.report_ms.repositories.CompaniesRepository;
import com.debuggeandoideas.report_ms.streams.ReportPublisher;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService{
    private final CompaniesRepository companiesRepository;
    private final ReportHelper reportHelper;
    private final CompaniesFallbackRepository companiesFallbackRepository;
    private final Resilience4JCircuitBreakerFactory circuitBreakerFactory;
    private final ReportPublisher reportPublisher;


    @Override
    public String makeReport(String name) {
        var circuitBraker = this.circuitBreakerFactory.create("companies-circuitbreaker");
        return circuitBraker.run(
                () -> this.makeReportMain(name),
                throwable -> this.makeReportFallback(name, throwable)
        );
    }

    @Override
    public String saveReport(String report) {
        System.out.println("Entering saveReport method");
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
        System.out.println("Sending report to consumer");
        this.reportPublisher.publishReport(report);
        System.out.println("Saving report");
        this.companiesRepository.postByName(company);
        return "Saved";
    }

    @Override
    public void deleteReport(String name) {
        this.companiesRepository.deleteByName(name);
    }

    /**
     * If Everything is good, it will execute
     * @param name
     * @return
     */
    private String makeReportMain(String name) {
        return reportHelper.readTemplate(this.companiesRepository.getByName(name).orElseThrow());
    }

    /**
     * If there is a problem, circuit breaker will use it
     * @param name
     * @param error
     * @return
     */
    private String makeReportFallback(String name, Throwable error) {
        log.warn(error.getMessage());
        return reportHelper.readTemplate(this.companiesFallbackRepository.getByName(name));
    }
}
