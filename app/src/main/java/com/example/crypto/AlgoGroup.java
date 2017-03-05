package com.example.crypto;

/**
 * Created by Tobias on 30.04.16.
 */
public class AlgoGroup {

    public static final AlgoGroup SYMMETRIC = new AlgoGroup("Symmetric", "Symmetrische Verschlüsselung");
    public static final AlgoGroup ASYMMETRIC = new AlgoGroup("Asymmetric", "Asymmetrische Verschlüsselung");

    private static final String[] SPINNERGROUPS = { SYMMETRIC.getSpinnerName(), ASYMMETRIC.getSpinnerName() };

    private String _name;
    private String _spinnerName;

    private AlgoGroup(String name, String spinnerName) {
        _name = name;
        _spinnerName = spinnerName;
    }

    public String getName() {
        return _name;
    }

    public String getSpinnerName() {
        return _spinnerName;
    }

    public static String[] getSpinnerNames() {
        return SPINNERGROUPS;
    }

    @Override
    public String toString() {
        return "Name " + _name + "\tSpinner Name " + _spinnerName;
    }
}
