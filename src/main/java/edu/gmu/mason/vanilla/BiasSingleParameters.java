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

import edu.gmu.mason.vanilla.utils.CustomConversionHandler;

/**
 * General description_________________________________________________________
 * This is a data structure class to keep parameters of reporting probabilities. The values are
 *  automatically loaded from a config file
 *
 */
public class BiasSingleParameters extends AnnotatedPropertied {

    // CONSTANTS
    public static final String DEFAULT_BIAS_FILENAME = "bias.single.properties";

    @EditableProperty(group = "About", description = "Description", lower = "", upper = "", readOnly = true)
    public static final String description = "This is a simple model, assigning the probability of reporting based on the setups of each bias type bellow separately.";

    @EditableProperty(group = "About", description = "Example", lower = "", upper = "", readOnly = true)
    public static final String example = "With the default setups, A White Male Bachelor with Age over 50 and Income less than 70k would have a 5 separate reporting probability: "
            + "0.8(Age), 0.8(Education), 0.5(Gender), 0.3(Income), and 0.3(Race)";

    @EditableProperty(group = "Init", description = "Activate single bias simulation", lower = "", upper = "", readOnly = false)
    public boolean activate;
    @EditableProperty(group = "Init", description = "Age", lower = "", upper = "", readOnly = false)
    public String ageProb;

    @EditableProperty(group = "Init", description = "Gender", lower = "", upper = "", readOnly = false)
    public String genderProb;

    @EditableProperty(group = "Init", description = "Education", lower = "", upper = "", readOnly = false)
    public String eduProb;

    @EditableProperty(group = "Init", description = "Income(k)", lower = "", upper = "", readOnly = false)
    public String incomeProb;

    @EditableProperty(group = "Init", description = "Race", lower = "", upper = "", readOnly = false)
    public String raceProb;

    @EditableProperty(group = "Init", description = "Hispanic", lower = "", upper = "", readOnly = false)
    public String hispanicProb;

    @EditableProperty(group = "Other Possible Biases", description = "Household Income(k)", lower = "", upper = "", readOnly = false)
    public String hhIncomeProb;

    @EditableProperty(group = "Other Possible Biases", description = "Residency", lower = "", upper = "", readOnly = false)
    public String residencyProb;

    @EditableProperty(group = "Other Possible Biases", description = "Living area", lower = "", upper = "", readOnly = false)
    public String livingAreaProb;


    public BiasSingleParameters() {
    }

    public BiasSingleParameters(String fileName)
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

        Field[] fields = BiasSingleParameters.class.getDeclaredFields();
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

        Field[] fields = BiasSingleParameters.class.getDeclaredFields();
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
        activate = false;
        ageProb = "";
        eduProb = "";
        genderProb = "";
        incomeProb = "";
        raceProb = "";
        hispanicProb = "";
        livingAreaProb = "";
        residencyProb = "";
        hhIncomeProb = "";
    }
}
