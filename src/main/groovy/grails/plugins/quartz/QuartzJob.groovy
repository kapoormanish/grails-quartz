/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugins.quartz

import grails.core.GrailsApplication
import grails.util.Holders
import grails.web.api.WebAttributes
import groovy.transform.CompileStatic
import org.quartz.JobDataMap
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.SimpleTrigger
import org.quartz.Trigger
import org.quartz.spi.MutableTrigger
import org.springframework.util.Assert

trait QuartzJob implements WebAttributes {
    private static Scheduler internalScheduler
    private static GrailsJobClass internalJobArtefact

    static triggerNow(Map params = null) {
        def jobArtefact = internalGetJobArtefact()
        def scheduler = internalGetScheduler()
        scheduler.triggerJob(new JobKey(this.getName(), jobArtefact.group), params ? new JobDataMap(params) : null)
    }

    static schedule(Long repeatInterval, Integer repeatCount = SimpleTrigger.REPEAT_INDEFINITELY, Map params = null) {
        def jobArtefact = internalGetJobArtefact()
        internalScheduleTrigger(TriggerUtils.buildSimpleTrigger(this.getName(), jobArtefact.group, repeatInterval, repeatCount), params)
    }

    static schedule(Date scheduleDate, Map params = null) {
        def jobArtefact = internalGetJobArtefact()
        internalScheduleTrigger(TriggerUtils.buildDateTrigger(this.getName(), jobArtefact.group, scheduleDate), params)
    }

    static schedule(Trigger trigger, Map params = null) {
        def jobArtefact = internalGetJobArtefact()
        def jobKey = new JobKey(this.getName(), jobArtefact.group)
        Assert.isTrue trigger.jobKey == jobKey || (trigger instanceof MutableTrigger),
                "The trigger job key is not equal to the job key or the trigger is immutable"

        trigger.jobKey = jobKey

        if (params) {
            trigger.jobDataMap.putAll(params)
        }
        def scheduler = internalGetScheduler()
        scheduler.scheduleJob(trigger)
    }

    private static internalScheduleTrigger(Trigger trigger, Map params = null) {
        if (params) {
            trigger.jobDataMap.putAll(params)
        }
        Scheduler scheduler = internalGetScheduler()
        scheduler.scheduleJob(trigger)
    }

    @CompileStatic
    private static Scheduler internalGetScheduler() {
        if(!internalScheduler) {
            def applicationContext = Holders.applicationContext
            internalScheduler = applicationContext.getBean('quartzScheduler', Scheduler)
        }
        internalScheduler
    }

    @CompileStatic
    private static GrailsJobClass internalGetJobArtefact() {
        if(!internalJobArtefact) {
            def applicationContext = Holders.applicationContext
            def grailsApplication = applicationContext.getBean('grailsApplication', GrailsApplication)
            internalJobArtefact = (GrailsJobClass)grailsApplication.getArtefact(DefaultGrailsJobClass.JOB, this.getName())
        }
        internalJobArtefact
    }
}