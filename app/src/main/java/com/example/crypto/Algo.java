package com.example.crypto;

/**
 * Created by Tobias on 30.04.16.
 * Diese Klasse nutzt ein statisches Pattern.
 */
public class Algo {

    public static final Algo AES = new Algo("AES", AlgoGroup.SYMMETRIC);
    public static final Algo DES = new Algo("DES", AlgoGroup.SYMMETRIC);
    public static final Algo RSA = new Algo("RSA", AlgoGroup.ASYMMETRIC);
    public static final Algo BLOWFISH = new Algo("BLOWFISH", AlgoGroup.SYMMETRIC);

    private static final Algo[] ALGOS = {AES, DES, RSA, BLOWFISH};

    private String _name;
    private AlgoGroup _algoGroup;

    private Algo(String name, AlgoGroup algoGroup) {
        _name = name;
        _algoGroup = algoGroup;
    }

    public String getName() {
        return _name;
    }

    public AlgoGroup getAlgoGroup() {
        return _algoGroup;
    }

    // Feld Algos soll unveränderbar sein.
    public Algo[] getAlgos() {
        return ALGOS;
    }

    public boolean isSymmetric() {
        return _algoGroup == AlgoGroup.SYMMETRIC;
    }

    public static Algo getAlgo(String name) {
        for (Algo algo : ALGOS) {
            if (algo.getName().equals(name)) {
                return algo;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Name " + _name + "\tAlgo Gruppe " + _algoGroup;
    }
}
