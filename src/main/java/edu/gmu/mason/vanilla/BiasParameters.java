package edu.gmu.mason.vanilla;

import edu.gmu.mason.vanilla.utils.CustomConversionHandler;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.joda.time.LocalDateTime;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * General description_________________________________________________________
 * This is a data structure class to keep parameters for a log regression model. The values are
 *  automatically loaded from a config file
 *
 */
public class BiasParameters extends AnnotatedPropertied {

    // CONSTANTS
    public static final String DEFAULT_BIAS_FILENAME = "bias.properties";

    @EditableProperty(group = "About", description = "Description", lower = "", upper = "", readOnly = true)
    public static final String description = "This is a logistic regression model to calculate the probability of reporting the infectious with the consideration of multivariate biases.";
    @EditableProperty(group = "About", description = "Probability Calculation Formula", lower = "", upper = "", readOnly = true)
    public static final String formula = "numerator = inspector * (ALL relative odds ratios), probability = numerator/(1+numerator)";
    @EditableProperty(group = "About", description = "Example", lower = "", upper = "", readOnly = true)
    public static final String example = "With the default setups, A White Male with Age over 50 and Income less than 70k would have a reporting probability:     (4.540 * (0.438*0.289*3.571))/[1 + (4.540 * (0.438*0.289*3.571))] = 0.672";


    @EditableProperty(group = "Init", description = "Biases to Consider", lower = "", upper = "", readOnly = false)
    public String biasConsideration;


    // From Reference
    @EditableProperty(group = "Intercepts", description = "Intercepts", lower = "0.0", upper = "1000.0", readOnly = false)
    public double intercept;

    @EditableProperty(group = "Odds Ratio", description = "Vulnerability: Age > 50", lower = "0.0", upper = "1000.0", readOnly = false)
    public double vulnerability;
    @EditableProperty(group = "Odds Ratio", description = "Income: > 70k", lower = "0.0", upper = "1000.0", readOnly = false)
    public double income;
    @EditableProperty(group = "Odds Ratio", description = "Race: White", lower = "0.0", upper = "1000.0", readOnly = false)
    public double raceWhite;
    @EditableProperty(group = "Odds Ratio", description = "Gender: Male", lower = "0.0", upper = "1000.0", readOnly = false)
    public double genderMale;


    // For Future Research
    @EditableProperty(group = "Other Possible Biases", description = "Education: Bachelor", lower = "0.0", upper = "1000.0", readOnly = false)
    public double eduBachelor;
    @EditableProperty(group = "Other Possible Biases", description = "Hispanic", lower = "0.0", upper = "1000.0", readOnly = false)
    public double hispanic;

    @EditableProperty(group = "Other Possible Biases", description = "Household Income: > 100k", lower = "0.0", upper = "1000.0", readOnly = false)
    public double hh_income;
    @EditableProperty(group ="Other Possible Biases", description = "Living area: > 30 sqm", lower = "0.0", upper = "1000.0", readOnly = false)
    public double livingArea;
    @EditableProperty(group ="Other Possible Biases", description = "Residency: Registered Inside Province", lower = "0.0", upper = "1000.0", readOnly = false)
    public double resInProvince;

    public BiasParameters() {
    }

    public BiasParameters(String fileName)
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

        Field[] fields = BiasParameters.class.getDeclaredFields();
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

        Field[] fields = BiasParameters.class.getDeclaredFields();
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
        biasConsideration = "Vulnerability/Gender/Race/Income";
        intercept = 4.540417;
        vulnerability = 3.571090;
        income = 2.470556;
        raceWhite = 0.288743;
        genderMale = 0.437807;
    }

}
