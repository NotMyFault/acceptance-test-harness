package plugins;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.jenkinsci.test.acceptance.po.*;
import org.jenkinsci.test.acceptance.po.stageview.StageView;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Created by boris on 17.04.17.
 * Base Implementation of the stageview test as a component. Important aspect of this testclass is the correct
 * visualisation depending of stages and builds (matrix).
 *
 * TODO: Build po for / in progress
 */
@WithPlugins("workflow-aggregator")
public class StageViewTest extends AbstractJUnitTest{


    public static final String SINGLE_JOB = "stageview_plugin/single_job.txt";
    public static final String MULTI_JOB = "stageview_plugin/multi_job.txt";
    public static final String MUTLI_JOB_FAIL = "stageview_plugin/multi_job_fail.txt";
    public static final String MUTLI_JOB_ABORTED = "stageview_plugin/multi_job_aborted.txt";
    public static final String MUTLI_JOB_UNSTABLE = "stageview_plugin/multi_job_unstable.txt";

    public static final String JOB_PATH = "/job/Pipeline-Test/";

    private PageObject context;
    private String path;
    private StageView stageView;

    /**
     * This tests create a simple stage. It checks if after the first build the stage view is now part of the job page.
     * @throws Exception
     */
    @Test
    public void jobShouldContainStageview() throws Exception {
        WorkflowJob job = this.saveWorkflowJobWithFile(SINGLE_JOB);
        Build build = job.startBuild().shouldSucceed();
        job.open();
        stageView = new StageView(job, JOB_PATH);
        assertThat(stageView.getRootElementName().getText(),containsString("Stage View"));
    }

    /**
     * This tests verfies the hieght of the diplay. The standard hieght is 11 of the maximum builds dislayed.
     */
    @Test
    public void multiBuildJobShouldContainCorrectNumberOfJobsBuilt() {
        WorkflowJob job = this.saveWorkflowJobWithFile(SINGLE_JOB);
        Build build = null;
        for (int i = 0; i < 8; i++) {
            build = job.startBuild().shouldSucceed();
        }
        assertThat(build,notNullValue());
        job.open();

        stageView = new StageView(job, JOB_PATH);
        assertThat(stageView.getAllStageViewJobs().size(),is(8)); //as not max display

        for (int i = 0; i < 10; i++) {
            build = job.startBuild().shouldSucceed();
        }
        assertThat(build,notNullValue());
        job.open();
        stageView = new StageView(job, JOB_PATH);
        assertThat(stageView.getAllStageViewJobs().size(),is(11));//max diplay is 11
    }

    /**
     * Test validates against the current build number. Every row(aka build) contains the correct build number.
     */
    public void jobNumberShouldbeCorrect() {
            WorkflowJob job = this.saveWorkflowJobWithFile(SINGLE_JOB);
            Build build = job.startBuild().shouldFail();
            job.open();
            job.getNavigationLinks();
            stageView = new StageView(job, JOB_PATH);
            WebElement webElement = this.driver.findElement(By.xpath("//*[@id=\"pipeline-box\"]/div/div/table/tbody[2]/tr[1]/td[1]/div/div/div[1]/span"));
            assertThat(webElement.getText(),containsString(String.valueOf(build.getNumber())));
    }

    /**
     * Does check multiple jobs in the stage view.
     * @throws Exception
     */
    @Test
    public void stageViewContainsMultipleStages() throws Exception {
        WorkflowJob job = this.saveWorkflowJobWithFile(MULTI_JOB);
        Build build = job.startBuild().shouldSucceed();
        job.open();
        stageView = new StageView(job, JOB_PATH);
        assertThat(stageView.getStageViewHeadlines().get(0).toString(),containsString("Clone sources"));
        assertThat(stageView.getStageViewHeadlines().get(1).toString(),containsString("Build"));
    }

    /**
     * Does check multiple jobs in the stage view. One with a failed, and one with a success.
     * @throws Exception
     */
    @Test
    public void stageViewContainsMultipleStagesWithFail() throws Exception {
        WorkflowJob job = this.saveWorkflowJobWithFile(MUTLI_JOB_FAIL);
        Build build = job.startBuild().shouldFail();
        job.open();
        job.getNavigationLinks();
        stageView = new StageView(job, JOB_PATH);
        String firstJob = stageView.getLatestBuild().getStageViewItem(0).toString();
        String secondJob = stageView.getLatestBuild().getStageViewItem(1).toString();
        assertThat(stageView.getLatestBuild().getCssClasses(),containsString("FAILED"));
        assertThat(firstJob,containsString("ms"));
        assertThat(secondJob,containsString("failed"));
    }

    /**
     * Does check multiple jobs in the stage view. One with a unstable, and one with a success. Unstable jobs
     * are represented with yellow color and represented with the css class "UNSTABLE".
     * @throws Exception
     */
    @Test
    public void stageViewContainsMultipleStagesWithUnstable() throws Exception {
        WorkflowJob job = this.saveWorkflowJobWithFile(MUTLI_JOB_UNSTABLE);
        Build build = job.startBuild().shouldBeUnstable();
        job.open();
        job.getNavigationLinks();
        stageView = new StageView(job, JOB_PATH);
        String firstJob = stageView.getLatestBuild().getStageViewItem(0).toString();
        String secondJob = stageView.getLatestBuild().getStageViewItem(1).toString();
        assertThat(stageView.getLatestBuild().getCssClasses(),containsString("UNSTABLE"));
        assertThat(firstJob,containsString("ms"));
    }

    /**
     * Does check multiple jobs in the stage view. One with a success, and one with aborted.
     * Aborted jobs are not represented in the satgeview. They are also shown green.
     * @throws Exception
     */
    @Test
    public void stageViewContainsMultipleStagesWithAborted() throws Exception {
        WorkflowJob job = this.saveWorkflowJobWithFile(MUTLI_JOB_ABORTED);
        Build build = job.startBuild().shouldAbort();
        job.open();
        job.getNavigationLinks();
        stageView = new StageView(job, JOB_PATH);
        String firstJob = stageView.getLatestBuild().getStageViewItem(0).toString();
        String secondJob = stageView.getLatestBuild().getStageViewItem(1).toString();
        assertThat(stageView.getLatestBuild().getCssClasses(),containsString("ABORTED"));
        assertThat(firstJob,containsString("ms"));
    }

    /**
     * Helper method to convenient located a file int he ressource folder
     * @param fileName the naame of the file including path
     * @return return the file content as a String
     */
    private String readFromRessourceFolder(String fileName) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        return  new BufferedReader(new InputStreamReader(classloader.getResourceAsStream(fileName)))
                .lines().collect(Collectors.joining("\n"));
    }


    /**
     * Helper Method for Workflow job generation. The filename represents
     * the File to be read as the pipeline definition file
     * @param fileName the naame of the file including path
     * @return return the newly generated workflow job with a defined pipeline
     */
    private WorkflowJob saveWorkflowJobWithFile(String fileName) {
        WorkflowJob job = jenkins.jobs.create(WorkflowJob.class);
        job.script.set(readFromRessourceFolder(fileName));
        job.sandbox.check();
        job.save();
        return job;
    }

}
