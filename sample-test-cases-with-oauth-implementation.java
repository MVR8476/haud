package com.haud.test.svalinn.service.protocol.controller.datadictionary;

import static org.junit.Assert.fail;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haud.lib.authentication.resource.test.EnableMockAuthService;
import com.haud.lib.authentication.resource.test.MockAuthService;
import com.haud.svalinn.lib.dto.protocol.datadictionary.ApplicationIDDTO;
import com.haud.svalinn.lib.dto.protocol.datadictionary.CommandCodeGroupDTO;
import com.haud.svalinn.service.protocol.Application;
@TestPropertySource(locations = "classpath:test.properties")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Application.class)
@SpringBootTest
@EnableMockAuthService
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ApplicationIDControllerTest {

	private MockMvc mockMvc;
	
	private static final String[] DEFAULT_AUTH_SCOPE = {"applicationId:CREATE","applicationId:UPDATE","applicationId:DELETE","applicationId:READ" };
    private static final String AUTHORIZATION_HEADER_VALUE = "Bearer %s";
    private static final String AUTH_HEADER = "Authorization";
    private static String tokenOk; 
    
	@Autowired
	MockAuthService mockAuthService;
	
	@Autowired
	FilterChainProxy springSecurityFilterChain;
	
	@Autowired
    private WebApplicationContext wac;
	
	ObjectMapper mapper = new ObjectMapper();
	
	private String url="/application";
	
	@Before
	public void setup() throws JsonProcessingException {
		final Map<String, Object> defaultOverrides = new HashMap<>();
		defaultOverrides.put("scope", Arrays.asList(DEFAULT_AUTH_SCOPE));
		defaultOverrides.put("user-name", "joker");
		defaultOverrides.put("client-id", "joker-100");
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).
                apply(springSecurity(springSecurityFilterChain)).build();
        tokenOk = String.format(AUTHORIZATION_HEADER_VALUE, mockAuthService.generateValidToken(
                "test",defaultOverrides,"SVALINN")); 
	}
	
	@Test  
	public void t01CreateApplication() throws Exception {
		try {
		ApplicationIDDTO applicationIDDTO = new ApplicationIDDTO();
		applicationIDDTO.setName("Application Test");
		
		CommandCodeGroupDTO commandCodeGroupDTO = new CommandCodeGroupDTO();
		commandCodeGroupDTO.setName("COMMAND CODE GROUP 1 Test");
		commandCodeGroupDTO.setPredefined(0);
		commandCodeGroupDTO.setId(522L);
		
		applicationIDDTO.setCommandCodeGroup(commandCodeGroupDTO);
		applicationIDDTO.setId(7L);
		applicationIDDTO.setPredefined(0);
		applicationIDDTO.setAppId(111L);
	
			
		
		mockMvc.perform(MockMvcRequestBuilders.post(url)
											        .contentType(MediaType.APPLICATION_JSON).header(AUTH_HEADER, tokenOk)
											        .content(mapper.writeValueAsString(applicationIDDTO))
													.accept(MediaType.APPLICATION_JSON))
													.andExpect(status().is(200))
													.andDo(print());
		} catch (Exception e) {
			   fail(e.toString());
		}
	}
	 
}
