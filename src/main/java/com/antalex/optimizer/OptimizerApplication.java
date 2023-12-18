package com.antalex.optimizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.antalex")
public class OptimizerApplication {
    public static void main(String[] args) {
//        long allTimeMillis = System.currentTimeMillis();
//        long timeMillis = System.currentTimeMillis();
        /*ApplicationContext context = */SpringApplication.run(OptimizerApplication.class, args);

        /*
        timeMillis = System.currentTimeMillis() - timeMillis;


        System.out.println("AAA START! time = " + timeMillis);


        timeMillis = System.currentTimeMillis();
        DataSource dataSource = context.getBean("getDataSource", DataSource.class);
        timeMillis = System.currentTimeMillis() - timeMillis;

        System.out.println("AAA getBean time = " + timeMillis);
        try {
            timeMillis = System.currentTimeMillis();

            Connection connection = dataSource.getConnection();
            Statement dbStatement = connection.createStatement();

            timeMillis = System.currentTimeMillis() - timeMillis;
            System.out.println("AAA connection time = " + timeMillis);

            timeMillis = System.currentTimeMillis();


            String query = "INSERT INTO IBS.Z#VND_ADD_PARAMS (ID, C_PARENT_ID, C_CODE, C_VALUE) VALUES (SEQ_ID.NEXTVAL, ?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(query);

//            StringBuilder sql = new StringBuilder("begin\n");



            for (int i = 0; i < 10000; i++) {

//                dbStatement.executeQuery("INSERT INTO IBS.Z#VND_ADD_PARAMS (ID, C_PARENT_ID, C_CODE, C_VALUE) VALUES (SEQ_ID.Nextval, 'TEST', SEQ_ID.Nextval, '2')");

//                sql = sql.append("INSERT INTO IBS.Z#VND_ADD_PARAMS (ID, C_PARENT_ID, C_CODE, C_VALUE) VALUES (SEQ_ID.NEXTVAL, 'TEST', SEQ_ID.Nextval, '2');\n");

                preparedStatement.setString(2, "TEST");
                preparedStatement.setString(2, "С_CODE" + Integer.toString(i));
                preparedStatement.setString(3, "VALUE" + Integer.toString(i));
                preparedStatement.addBatch();
            }

//            sql = sql.append("end;\n");
            timeMillis = System.currentTimeMillis() - timeMillis;
            System.out.println("AAA PREPARE INSERT time = " + timeMillis);

            timeMillis = System.currentTimeMillis();
//            dbStatement.executeQuery(sql.toString());

            preparedStatement.executeBatch();


            timeMillis = System.currentTimeMillis() - timeMillis;
            System.out.println("AAA INSERT time = " + timeMillis);





            timeMillis = System.currentTimeMillis();
            connection.close();
            timeMillis = System.currentTimeMillis() - timeMillis;
            System.out.println("AAA CLOSE time = " + timeMillis);
        } catch (Exception err) {
            throw new RuntimeException(err);
        }

        allTimeMillis = System.currentTimeMillis() - allTimeMillis;
        System.out.println("AAA FINISH! allTime = " + allTimeMillis);

/*
        try {
            Document doc = Jsoup.connect("https://www.dohod.ru/ik/analytics/dividend/").get();
            Element element = doc.getElementById("table-dividend");
            System.out.println("AAA html " + element.html());
        } catch (IOException e) {

        }
*/
    }

}
