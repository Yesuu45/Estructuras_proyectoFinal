package co.edu.uniquindio.poo.ui;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(new FiltroSesion()).addPathPatterns("/api/**");
    }
}
