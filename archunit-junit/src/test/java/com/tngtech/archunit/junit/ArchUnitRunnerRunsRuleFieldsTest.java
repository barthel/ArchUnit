package com.tngtech.archunit.junit;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.junit.ArchUnitRunner.SharedCache;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.tngtech.archunit.core.DescribedPredicate.all;
import static com.tngtech.archunit.core.TestUtils.javaClasses;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleFieldsTest.IgnoredArchTest.RULE_ONE_IN_IGNORED_TEST;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleFieldsTest.IgnoredArchTest.RULE_TWO_IN_IGNORED_TEST;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleFieldsTest.SomeArchTest.FAILING_FIELD_NAME;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleFieldsTest.SomeArchTest.IGNORED_FIELD_NAME;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleFieldsTest.SomeArchTest.SATISFIED_FIELD_NAME;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleFieldsTest.WrongArchTest.NO_RULE_AT_ALL_FIELD_NAME;
import static com.tngtech.archunit.junit.ArchUnitRunnerRunsRuleFieldsTest.WrongArchTest.WRONG_MODIFIER_FIELD_NAME;
import static com.tngtech.archunit.lang.ArchRule.rule;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArchUnitRunnerRunsRuleFieldsTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private SharedCache cache;
    @Mock
    private ClassCache classCache;

    @Mock
    private RunNotifier runNotifier;

    @Captor
    private ArgumentCaptor<Description> descriptionCaptor;

    @Captor
    private ArgumentCaptor<Failure> failureCaptor;

    @InjectMocks
    private ArchUnitRunner runner = ArchUnitRunnerTestUtils.newRunnerFor(SomeArchTest.class);

    private JavaClasses cachedClasses = javaClasses(ArchUnitRunnerRunsRuleFieldsTest.class);

    @Before
    public void setUp() {
        when(cache.get()).thenReturn(classCache);
        when(classCache.getClassesToAnalyseFor(any(Class.class))).thenReturn(cachedClasses);
    }

    @Test
    public void should_find_children() throws Exception {
        assertThat(runner.getChildren()).as("Rules defined in Test Class").hasSize(3);
    }

    @Test
    public void should_start_rule() {
        ArchTestExecution satisfiedRule = getRule(SATISFIED_FIELD_NAME);

        runner.runChild(satisfiedRule, runNotifier);

        verify(runNotifier).fireTestStarted(descriptionCaptor.capture());
        assertThat(descriptionCaptor.getValue().toString()).contains(SATISFIED_FIELD_NAME);
    }

    @Test
    public void should_accept_satisfied_rule() {
        ArchTestExecution satisfiedRule = getRule(SATISFIED_FIELD_NAME);

        runner.runChild(satisfiedRule, runNotifier);

        verify(runNotifier).fireTestFinished(descriptionCaptor.capture());
        assertThat(descriptionCaptor.getValue().toString()).contains(SATISFIED_FIELD_NAME);
    }

    @Test
    public void should_fail_on_wrong_field_visibility() throws InitializationError {
        ArchUnitRunner runner = new ArchUnitRunner(WrongArchTest.class);

        thrown.expectMessage("With @ArchTest annotated members must be public and static");

        runner.runChild(ArchUnitRunnerTestUtils.getRule(WRONG_MODIFIER_FIELD_NAME, runner), runNotifier);
    }

    @Test
    public void should_fail_on_wrong_field_type() throws InitializationError {
        ArchUnitRunner runner = new ArchUnitRunner(WrongArchTest.class);

        thrown.expectMessage("With @ArchTest annotated members must be public and static");

        runner.runChild(ArchUnitRunnerTestUtils.getRule(NO_RULE_AT_ALL_FIELD_NAME, runner), runNotifier);
    }

    @Test
    public void should_fail_unsatisfied_rule() {
        ArchTestExecution satisfiedRule = getRule(FAILING_FIELD_NAME);

        runner.runChild(satisfiedRule, runNotifier);

        verify(runNotifier).fireTestFailure(failureCaptor.capture());
        Failure failure = failureCaptor.getValue();
        assertThat(failure.getDescription().toString()).contains(FAILING_FIELD_NAME);
        assertThat(failure.getException()).isInstanceOf(AssertionError.class);
    }

    @Test
    public void should_skip_ignored_rule() {
        ArchTestExecution satisfiedRule = getRule(IGNORED_FIELD_NAME);

        runner.runChild(satisfiedRule, runNotifier);

        verify(runNotifier).fireTestIgnored(descriptionCaptor.capture());
        assertThat(descriptionCaptor.getValue().toString()).contains(IGNORED_FIELD_NAME);
    }

    @Test
    public void should_skip_ignored_test() throws InitializationError {
        ArchUnitRunner runner = new ArchUnitRunner(IgnoredArchTest.class);

        runner.runChild(ArchUnitRunnerTestUtils.getRule(RULE_ONE_IN_IGNORED_TEST, runner), runNotifier);
        runner.runChild(ArchUnitRunnerTestUtils.getRule(RULE_TWO_IN_IGNORED_TEST, runner), runNotifier);

        verify(runNotifier, times(2)).fireTestIgnored(descriptionCaptor.capture());

        assertThat(descriptionCaptor.getAllValues()).extractingResultOf("getMethodName")
                .contains(RULE_ONE_IN_IGNORED_TEST, RULE_TWO_IN_IGNORED_TEST);
    }

    private ArchTestExecution getRule(String name) {
        return ArchUnitRunnerTestUtils.getRule(name, runner);
    }

    @AnalyseClasses(packages = "some.pkg")
    public static class SomeArchTest {
        static final String SATISFIED_FIELD_NAME = "someSatisfiedRule";
        static final String FAILING_FIELD_NAME = "someFailingRule";
        static final String IGNORED_FIELD_NAME = "someIgnoredRule";

        @ArchTest
        public static final ArchRule<JavaClass> someSatisfiedRule =
                rule(all(JavaClass.class)).should("satisfy something").assertedBy(ArchUnitRunnerTestUtils.ALWAYS_SATISFIED);

        @ArchTest
        public static final ArchRule<JavaClass> someFailingRule =
                rule(all(JavaClass.class)).should("satisfy something, but don't").assertedBy(never(ArchUnitRunnerTestUtils.ALWAYS_SATISFIED));

        @ArchIgnore
        @ArchTest
        public static final ArchRule<JavaClass> someIgnoredRule =
                rule(all(JavaClass.class)).should("satisfy something, but don't").assertedBy(never(ArchUnitRunnerTestUtils.ALWAYS_SATISFIED));
    }

    @AnalyseClasses(packages = "some.pkg")
    public static class WrongArchTest {
        static final String WRONG_MODIFIER_FIELD_NAME = "ruleWithWrongModifier";
        static final String NO_RULE_AT_ALL_FIELD_NAME = "noRuleAtAll";

        @ArchTest
        private ArchRule<JavaClass> ruleWithWrongModifier =
                rule(all(JavaClass.class)).should("satisfy something").assertedBy(ArchUnitRunnerTestUtils.ALWAYS_SATISFIED);

        @ArchTest
        public static Object noRuleAtAll = new Object();
    }

    @ArchIgnore
    @AnalyseClasses(packages = "some.pkg")
    public static class IgnoredArchTest {
        static final String RULE_ONE_IN_IGNORED_TEST = "someRuleOne";
        static final String RULE_TWO_IN_IGNORED_TEST = "someRuleTwo";

        @ArchTest
        public static final ArchRule<JavaClass> someRuleOne =
                rule(all(JavaClass.class)).should("satisfy something, but don't").assertedBy(never(ArchUnitRunnerTestUtils.ALWAYS_SATISFIED));

        @ArchTest
        public static final ArchRule<JavaClass> someRuleTwo =
                rule(all(JavaClass.class)).should("satisfy something, but don't").assertedBy(never(ArchUnitRunnerTestUtils.ALWAYS_SATISFIED));
    }
}