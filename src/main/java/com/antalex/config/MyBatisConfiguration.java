package com.antalex.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.vtb.pmts.db.service.ShardDataBaseManager;

import javax.sql.DataSource;

@Configuration
@MapperScan("com.antalex.service.mapper")
public class MyBatisConfiguration {
    private ShardDataBaseManager dataBaseManager;
    private DataSource dataSource;

    @Autowired
    public MyBatisConfiguration(ShardDataBaseManager dataBaseManager) {
        this.dataSource = dataBaseManager.getDataSource();
    }

    @Bean
    public SqlSessionFactoryBean sqlSessionFactoryBean() throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        return sqlSessionFactoryBean;
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
