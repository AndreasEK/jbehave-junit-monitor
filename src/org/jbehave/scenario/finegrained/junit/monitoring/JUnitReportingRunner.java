package org.jbehave.scenario.finegrained.junit.monitoring;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.embedder.Embedder;
import org.jbehave.core.embedder.StoryRunner;
import org.jbehave.core.junit.JUnitStories;
import org.jbehave.core.model.Story;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

public class JUnitReportingRunner extends Runner {
    private List<Description> storyDescriptions;
    private Embedder configuredEmbedder;
    private List<String> storyPaths;
    private JUnitStories junitStories;
    private Configuration configuration;

    @SuppressWarnings("unchecked")
    public JUnitReportingRunner(Class<? extends JUnitStories> testClass) throws Throwable {

        junitStories = testClass.newInstance();
        configuredEmbedder = junitStories.configuredEmbedder();
        configuration = configuredEmbedder.configuration();

        Method method = testClass.getDeclaredMethod("storyPaths", (Class[]) null);
        method.setAccessible(true);
        storyPaths = ((List<String>) method.invoke(junitStories, (Object[]) null));

        storyDescriptions = buildDescriptionFromStories();

    }

    @Override
    public Description getDescription() {
        Description root = Description.createSuiteDescription(junitStories.getClass());
        root.getChildren().addAll(storyDescriptions);
        return root;
    }

    @Override
    public int testCount() {
        return storyDescriptions.size();
    }

    @Override
    public void run(RunNotifier notifier) {

        JUnitScenarioReporter reporter = new JUnitScenarioReporter(notifier, storyDescriptions.toArray(new Description[0]));

        StoryReporterBuilder reporterBuilder = new StoryReporterBuilder().withReporters(reporter);
        Configuration junitReportingConfiguration = junitStories.configuration().useStoryReporterBuilder(reporterBuilder);
        configuredEmbedder.useConfiguration(junitReportingConfiguration);

        try {
            configuredEmbedder.runStoriesAsPaths(storyPaths);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            configuredEmbedder.generateCrossReference();
        }
    }

    private List<Description> buildDescriptionFromStories() {
        JUnitDescriptionGenerator gen = new JUnitDescriptionGenerator();
        StoryRunner storyRunner = new StoryRunner();
        List<Description> storyDescriptions = new ArrayList<Description>();

        for (String storyPath : storyPaths) {
            Story parseStory = storyRunner.storyOfPath(configuration, storyPath);
            Description descr = gen.createDescriptionFrom(parseStory);
            storyDescriptions.add(descr);
        }
        return storyDescriptions;
    }
}