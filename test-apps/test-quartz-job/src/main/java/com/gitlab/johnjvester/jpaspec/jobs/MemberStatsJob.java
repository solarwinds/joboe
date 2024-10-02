package com.gitlab.johnjvester.jpaspec.jobs;

import com.gitlab.johnjvester.jpaspec.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@DisallowConcurrentExecution
public class MemberStatsJob implements Job {
    @Autowired
    private MemberService memberService;
    private static final String LONG_STRING = getLongString();
    private static final int RESULT_LENGTH = 10 * 1024;

    private static String getLongString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0 ; i < RESULT_LENGTH ; i ++) {
            builder.append('a');
        }
        return builder.toString();
    }

    @Override
    public void execute(JobExecutionContext context) {
        log.info("Job ** {} ** starting @ {}", context.getJobDetail().getKey().getName(), context.getFireTime());
        memberService.memberStats();
        log.info("Job ** {} ** completed.  Next job scheduled @ {}", context.getJobDetail().getKey().getName(), context.getNextFireTime());

        context.setResult(LONG_STRING);
    }
}
