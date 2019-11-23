package ru.avem.resonance.db;

import ru.avem.resonance.db.model.Account;
import ru.avem.resonance.db.model.Protocol;
import ru.avem.resonance.db.model.TestItem;

import java.io.File;
import java.util.ArrayList;

public abstract class DataBaseRepository {
    public static String DATABASE_NAME = "resonanceDataBase.db";
    static String DATABASE_URL = "jdbc:" + "sqlite:" + DATABASE_NAME;

    public static void init(boolean forceInit) {
        if (!new File(DATABASE_NAME).exists() || forceInit) {
            AccountRepository.createTable(Account.class);
            Account ivanov = new Account("ivanov", "1234", "Исполнитель-1", "148", "Иванов И. И.");
            AccountRepository.insertAccount(ivanov);
            Account petrov = new Account("petrov", "1234", "Исполнитель-2", "841", "Петров П. П.");
            AccountRepository.insertAccount(petrov);

            TestItemRepository.createTable();
            ArrayList<Double> timesResonance = new ArrayList<>();
            timesResonance.add(0, 1.0);
            ArrayList<Double> voltageResonance = new ArrayList<>();
            voltageResonance.add(0, 3.0);
            ArrayList<Double> speedResonance = new ArrayList<>();
            speedResonance.add(0, 2.0);
            ArrayList<Double> timesViu = new ArrayList<>();
            timesViu.add(0, 3.0);
            ArrayList<Double> voltageViu = new ArrayList<>();
            voltageViu.add(0, 4.0);
            ArrayList<Double> speedViu = new ArrayList<>();
            speedViu.add(0, 4.0);
            ArrayList<Double> timesViuDC = new ArrayList<>();
            timesViuDC.add(0, 5.0);
            ArrayList<Double> voltageViuDC = new ArrayList<>();
            voltageViuDC.add(0, 6.0);
            ArrayList<Double> speedViuDC = new ArrayList<>();
            speedViuDC.add(0, 6.0);
            TestItem testItem = new TestItem("Двигатель", timesResonance, voltageResonance, speedResonance,
                    timesViu, voltageViu, speedViu,
                    timesViuDC, voltageViuDC, speedViuDC);
            TestItemRepository.insertTestItem(testItem);

            ProtocolRepository.createTable();
            Protocol protocol = new Protocol("SN1", testItem, ivanov, petrov, System.currentTimeMillis());
            ProtocolRepository.insertProtocol(protocol);
        }
    }
}
