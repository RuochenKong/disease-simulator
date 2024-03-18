package edu.gmu.mason.vanilla;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.joda.time.LocalDateTime;

import edu.gmu.mason.vanilla.EditableProperty;
import edu.gmu.mason.vanilla.log.Skip;
import edu.gmu.mason.vanilla.utils.CustomConversionHandler;
import edu.gmu.mason.vanilla.utils.SimulationTimeStepSetting;


public class WorldBiasParameters extends AnnotatedPropertied {

    // CONSTANTS
    public static final String DEFAULT_BIAS_FILE_NAME = "parameters.bias";

    // AGE GROUPS
    @EditableProperty(group = "Age Group", description = "Age 15 to 19", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfAge15to19;
    @EditableProperty(group = "Age Group", description = "Age 20 to 24", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfAge20to24;
    @EditableProperty(group = "Age Group", description = "Age 25 to 29", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfAge25to29;
    @EditableProperty(group = "Age Group", description = "Age 30 to 34", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfAge30to34;
    @EditableProperty(group = "Age Group", description = "Age 35 to 39", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfAge35to39;
    @EditableProperty(group = "Age Group", description = "Age 40 to 44", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfAge40to44;
    @EditableProperty(group = "Age Group", description = "Age 45 to 49", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfAge45to49;
    @EditableProperty(group = "Age Group", description = "Age 50 to 54", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfAge50to54;
    @EditableProperty(group = "Age Group", description = "Age 55 to 59", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfAge55to59;
    @EditableProperty(group = "Age Group", description = "Age 60 to 64", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfAge60to64;

    // RACE
    @EditableProperty(group = "Race", description = "White Only", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfWhiteOnly;
    @EditableProperty(group = "Race", description = "Black Only", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfBlackOnly;
    @EditableProperty(group = "Race", description = "American Indian Only", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfAmerIndianOnly;
    @EditableProperty(group = "Race", description = "Asian Only", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfAsianOnly;
    @EditableProperty(group = "Race", description = "Pacific Island Only", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfPacIslandOnly;
    @EditableProperty(group = "Race", description = "Other Race", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfOtherRace;
    @EditableProperty(group = "Race", description = "More Than 2 Races", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfPlus2Races;

    // EDUCATION
    @EditableProperty(group="Education", description = "Low Education Level", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfLow;
    @EditableProperty(group="Education", description = "High School Or College", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfHighSchoolOrCollege;
    @EditableProperty(group="Education", description = "Bachelors", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfBachelors;
    @EditableProperty(group="Education", description = "Graduate", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfGraduate;

    // GENDER
    @EditableProperty(group = "Gender", description = "Male", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfMale;
    @EditableProperty(group = "Gender", description = "Female", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfFemale;

    // HISPANIC
    @EditableProperty(group = "Hispanic", description = "Hispanic", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfHispanic;
    @EditableProperty(group = "Hispanic", description = "Non-Hispanic", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfNonHispanic;

    // INCOME
    @EditableProperty(group = "Income", description = "Income Under 25 percentile", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfIncomeQ1;
    @EditableProperty(group = "Income", description = "Income Within 25 to 50 percentile", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfIncomeQ2;
    @EditableProperty(group = "Income", description = "Income Within 50 to 75 percentile", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfIncomeQ3;
    @EditableProperty(group = "Income", description = "Income Above 75 percentile", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfIncomeQ4;

    // REGION (Medium Income over a region)
    @EditableProperty(group = "Region", description = "Reporting Threshold of Income", lower = "0.0", upper = "1000000.0", readOnly = false)
    public double annualIncomeThres;
    @EditableProperty(group = "Region", description = "Reporting Rate Over the Threshold", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfAboveThres;

    @EditableProperty(group = "Region", description = "Reporting Rate Below the Threshold", lower = "0.0", upper = "1.0", readOnly = false)
    public double rateOfBelowThres;



    public WorldBiasParameters() {
    }

    public WorldBiasParameters(String fileName)
            throws IllegalArgumentException, IllegalAccessException, ConfigurationException {
        this();
        Parameters params = new Parameters();
        File propertiesFile = new File(fileName);

        CustomConversionHandler handler = new CustomConversionHandler();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(
                PropertiesConfiguration.class)
                .configure(params.fileBased().setFile(propertiesFile).setConversionHandler(handler)
                        .setListDelimiterHandler(new DefaultListDelimiterHandler(',')));
        Configuration conf = builder.getConfiguration();

        Field[] fields = WorldBiasParameters.class.getDeclaredFields();
        int mod;
        int skipMod = Modifier.STATIC | Modifier.VOLATILE | Modifier.TRANSIENT | Modifier.FINAL;
        for (int i = 0; i < fields.length; i++) {
            mod = fields[i].getModifiers();
            if ((mod & skipMod) == 0 || fields[i].getName().equals("a")) {
                String key = fields[i].getName();
                if (!conf.containsKey(key))
                    continue;
                Object value = conf.get((Class<?>) fields[i].getType(), key);

                fields[i].setAccessible(true);
                fields[i].set(this, value);
            }
        }
    }

    public void store(String fileName)
            throws IllegalArgumentException, IllegalAccessException, ConfigurationException, IOException {
        Parameters params = new Parameters();
        File propertiesFile = new File(fileName);
        if (!propertiesFile.exists())
            propertiesFile.createNewFile();

        FileBasedConfigurationBuilder<FileBasedConfiguration> builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(
                PropertiesConfiguration.class).configure(params.fileBased().setFile(propertiesFile));
        Configuration conf = builder.getConfiguration();

        Field[] fields = WorldBiasParameters.class.getDeclaredFields();
        int mod;
        int skipMod = Modifier.STATIC | Modifier.VOLATILE | Modifier.TRANSIENT | Modifier.FINAL;
        for (int i = 0; i < fields.length; i++) {
            mod = fields[i].getModifiers();
            if ((mod & skipMod) == 0) {
                String key = fields[i].getName();
                Object defaultValue = fields[i].get(this);
                if (defaultValue instanceof LocalDateTime) {
                    defaultValue = defaultValue.toString();
                }

                conf.setProperty(key, defaultValue);
            }
        }
        builder.save();
    }

    protected void initializationWithDefaultValues() {

        // AGE GROUP
        rateOfAge15to19 = 1.0;
        rateOfAge20to24 = 1.0;
        rateOfAge25to29 = 1.0;
        rateOfAge30to34 = 1.0;
        rateOfAge35to39 = 1.0;
        rateOfAge40to44 = 1.0;
        rateOfAge45to49 = 1.0;
        rateOfAge50to54 = 1.0;
        rateOfAge55to59 = 1.0;
        rateOfAge60to64 = 1.0;

        // RACE
        rateOfWhiteOnly = 1.0;
        rateOfBlackOnly = 1.0;
        rateOfAmerIndianOnly = 1.0;
        rateOfAsianOnly = 1.0;
        rateOfPacIslandOnly = 1.0;
        rateOfOtherRace = 1.0;
        rateOfPlus2Races = 1.0;

        // EDUCATION
        rateOfLow = 1.0;
        rateOfHighSchoolOrCollege = 1.0;
        rateOfBachelors = 1.0;
        rateOfGraduate = 1.0;

        // GENDER
        rateOfMale = 1.0;
        rateOfFemale = 1.0;

        // HISPANIC
        rateOfHispanic = 1.0;
        rateOfNonHispanic = 1.0;

        // INCOME
        rateOfIncomeQ1 = 1.0;
        rateOfIncomeQ2 = 1.0;
        rateOfIncomeQ3 = 1.0;
        rateOfIncomeQ4 = 1.0;

        // REGION
        annualIncomeThres = 1000.0;
        rateOfAboveThres = 1.0;
        rateOfBelowThres = 0;
    }
}
