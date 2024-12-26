package com.springbatchcourse.infrastructure.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public BatchConfiguration(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    @Bean
    public Job helloWorldJob() {
        SimpleJobBuilder simpleJobBuilder = new JobBuilder("helloWorldJob", jobRepository)
                .start(helloWorldStep());
        return simpleJobBuilder.build();
//        return new JobBuilder("helloWorldJob", jobRepository)
//                .start(helloWorldStep())
//                .build();
    }

    @Bean
    public Step helloWorldStep() {
        return new StepBuilder("helloWorldStep", jobRepository)
                .<String, String>chunk(10, transactionManager)
                .reader(new ItemReader<String>() {
                    @Override
                    public String read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
                        return "Hello world!";
                    }
                })
                .writer(items -> {
                    for (String item : items) {
                        System.out.println(item);
                    }
                })
                .build();
    }
}
