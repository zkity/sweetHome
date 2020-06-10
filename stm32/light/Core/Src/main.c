/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : main.c
  * @brief          : Main program body
  ******************************************************************************
  * @attention
  *
  * <h2><center>&copy; Copyright (c) 2020 STMicroelectronics.
  * All rights reserved.</center></h2>
  *
  * This software component is licensed by ST under BSD 3-Clause license,
  * the "License"; You may not use this file except in compliance with the
  * License. You may obtain a copy of the License at:
  *                        opensource.org/licenses/BSD-3-Clause
  *
  ******************************************************************************
  */
/**
 * 时钟及引脚配置
 *
 * 外部8MHz时钟，APB2时钟为56MHz,ADC为14MHz
 * ADC_CH1,ADC_CH2采样时间为71.5+12.5 Cycle
 * TIM3用于定时采集声音及光线强度，在APB2总线上，考虑到人听音范围为20Hz-20KHz，故设定Prescaler=14-1,Counter Peroid=100-1
 * 求得TIM的中断频率为 APB2Clock/((Prescaler+1)*(Counter Peroid+1))=20KHz,根据奈奎斯特采样定律应取40KHz,但受限于硬件设施，这里取10KHz
 * 同时ADC的转换时间为Sampling Time = (Cycle+12.5)/F × cn = 12us, 小于中断时间100us，每次中断AD转换可以完成
 * 用于驱动全彩LED的PWM输出引脚，其PWM占空比的计算为 PSB = Pulse/Counter Peroid，Plus对应到TIM3->CCRn
 *
 * PA9-USART1_TX: ZigBee_Rx-17
 * PA10-USART1_RX: ZigBee_Tx-16
 * PB3-GPIO_Output: ZigBee_Mode-13
 * PB4-GPIO_Output: ZigBee_RST
 *
 * PB10-USART3_TX: Debug_Rx
 * PB11-USART3_RX: Debug_Tx
 *
 * PB15-GPIO_Exit: TouchSwitch-IO
 * PB13-GPIO_Output: Relay-IN
 *
 * PA1-ADC1_IN1: LightSensor-S
 * PA2-ADC2_IN2: VoiceSensor-AO
 *
 * PA7-TIM3_CH2: FulColoredLED_R
 * PB0-TIM3_CH3: FulColoredLED_G
 * PB1-TIM3_CH4: FulColoredLED_B
 */
/* USER CODE END Header */

/* Includes ------------------------------------------------------------------*/
#include "main.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include "stdio.h"
#include "math.h"
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */

/* USER CODE END PTD */

/* Private define ------------------------------------------------------------*/
/* USER CODE BEGIN PD */

// 用于串口输出到ZigBee的数组的大小
#define UART_ZB_BUF_SIZE 10
// 用于串口输出到Log的数组大小
#define UART_LOG_BUF_SIZE 128

// 声音信号的暂存数组大小
#define VOICE_BUF_SIZE 100
#define VOICE_BUF_PER_SIZE 100

// 存储前ns声音信号的值
#define VOICE_STORE_N 3

// ZigBee 发送数据长度
#define ZB_SEND_DATA_LEN 8
// ZigBee 接收数据长度
#define ZB_RECEIVE_DATA_LEN 5

// 串口输出到ZigBee，DMA方式
#define sendToZBByDMA(...) HAL_UART_Transmit_DMA(&huart1,\
										(uint8_t*)uart_ZB_buf,\
										sprintf(uart_ZB_buf, __VA_ARGS__))
// 串口输出到ZigBee，IT方式
#define sendToZBByIT(...) HAL_UART_Transmit_IT(&huart1,\
										(uint8_t*)uart_ZB_buf,\
										sprintf(uart_ZB_buf, __VA_ARGS__))
// 串口输出到Log，DMA方式
#define sendToLogByDMA(...) HAL_UART_Transmit_DMA(&huart3,\
										(uint8_t*)uart_log_buf,\
										sprintf(uart_log_buf, __VA_ARGS__))
// 串口输出到Log，IT方式
#define sendToLogByIT(...) HAL_UART_Transmit_IT(&huart3,\
										(uint8_t*)uart_log_buf,\
										sprintf(uart_log_buf, __VA_ARGS__))
/* USER CODE END PD */

/* Private macro -------------------------------------------------------------*/
/* USER CODE BEGIN PM */

/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/
ADC_HandleTypeDef hadc1;
DMA_HandleTypeDef hdma_adc1;

TIM_HandleTypeDef htim3;

UART_HandleTypeDef huart1;
UART_HandleTypeDef huart3;
DMA_HandleTypeDef hdma_usart1_rx;
DMA_HandleTypeDef hdma_usart1_tx;
DMA_HandleTypeDef hdma_usart3_rx;
DMA_HandleTypeDef hdma_usart3_tx;

/* USER CODE BEGIN PV */

// 设备地址
const char DEVICE_ADDRESS = 'b';
// 设备类型
const char DEVICE_TYPE = 'b';

// 上次执行的命令代码
char cmd_idx = 0;

// 声控模式标志
uint8_t is_voice_light_light = 0;
// 进门开灯模式标志
uint8_t is_door_light = 0;

// ZigBee接收到数据的标志
uint8_t is_ZB_rec = 0;
// ZigBee接收数据缓存
uint8_t ZB_rec_buf[ZB_RECEIVE_DATA_LEN];
// 用于串口输出到ZigBee的数组的大小
char uart_ZB_buf[UART_ZB_BUF_SIZE];

// 用于串口输出到Log的数组
char uart_log_buf[UART_LOG_BUF_SIZE];

// 用于ADC转换存储的数组
uint32_t adc_buf[2];

// 用于记录当前1s内采集到的声音信号
uint8_t voice_s[VOICE_BUF_SIZE] = {0};
// 用于记录当前1/200s内采集到的声音信号
uint8_t voice_s_p[VOICE_BUF_PER_SIZE] = {0};
// 上述对应信号容器的索引值
uint8_t voice_s_idx = 0;
uint8_t voice_s_p_idx = 0;

// 用于存储前ns声音信号的最大值
uint8_t voice_t[VOICE_STORE_N] = {0};
uint8_t voice_t_idx = 0;

// 当前信号转换后的值
uint8_t light_current = 0;
uint8_t voice_current = 0;

/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
void SystemClock_Config(void);
static void MX_GPIO_Init(void);
static void MX_DMA_Init(void);
static void MX_ADC1_Init(void);
static void MX_TIM3_Init(void);
static void MX_USART1_UART_Init(void);
static void MX_USART3_UART_Init(void);
/* USER CODE BEGIN PFP */

// 控制全彩LED的函数，细致控制
// 该LED，RGB通道的额定电压为1v8-2v,3v2-3v4,3v2-3v4;最高亮度为1200,5000,3000
// 参数r,g,b取值为0-100,为对应通道的亮度
void setLED(uint8_t r, uint8_t g, uint8_t b){
	float nmbda_h = 0.8;
	float nmbda_r = 0.6;
	float nmbda_g = 0.4;
	float nmbda_b = 0.7;
	TIM3->CCR2 = floor(r*nmbda_h*nmbda_r);
	TIM3->CCR3 = floor(g*nmbda_h*nmbda_g);
	TIM3->CCR4 = floor(b*nmbda_h*nmbda_b);
}

// 按照预定状态控制LED
void setLevLED(uint8_t lev){
	uint8_t r=0,g=0,b=0;
	switch(lev){
		case 0: r=55, g=10, b=10;break;
		case 1: r=10, g=50, b=10;break;
		case 2: r=10, g=10, b=60;break;
		default: r=55, g=10, b=10;
	}
	TIM3->CCR2 = r;
	TIM3->CCR3 = g;
	TIM3->CCR4 = b;
}


/* USER CODE END PFP */

/* Private user code ---------------------------------------------------------*/
/* USER CODE BEGIN 0 */

/* USER CODE END 0 */

/**
  * @brief  The application entry point.
  * @retval int
  */
int main(void)
{
  /* USER CODE BEGIN 1 */

  /* USER CODE END 1 */
  

  /* MCU Configuration--------------------------------------------------------*/

  /* Reset of all peripherals, Initializes the Flash interface and the Systick. */
  HAL_Init();

  /* USER CODE BEGIN Init */

  /* USER CODE END Init */

  /* Configure the system clock */
  SystemClock_Config();

  /* USER CODE BEGIN SysInit */

  /* USER CODE END SysInit */

  /* Initialize all configured peripherals */
  MX_GPIO_Init();
  MX_DMA_Init();
  MX_ADC1_Init();
  MX_TIM3_Init();
  MX_USART1_UART_Init();
  MX_USART3_UART_Init();
  /* USER CODE BEGIN 2 */

  sendToLogByDMA("device init success!\n");
  HAL_Delay(20);
  // 开启时钟
  if(HAL_OK != HAL_TIM_Base_Start_IT(&htim3))
	  Error_Handler();

  // 开启全彩LED的PWM引脚
  TIM3->CCR2 = 60;
  TIM3->CCR3 = 40;
  TIM3->CCR4 = 30;
  HAL_TIM_PWM_Start(&htim3, TIM_CHANNEL_2);
  HAL_TIM_PWM_Start(&htim3, TIM_CHANNEL_3);
  HAL_TIM_PWM_Start(&htim3, TIM_CHANNEL_4);
  HAL_Delay(2000);
  TIM3->CCR2 = 30;
  TIM3->CCR3 = 40;
  TIM3->CCR4 = 50;
  // 串口1使用DMA方式循环接收数据
  HAL_UART_Receive_DMA(&huart1,\
		  	  	  	   ZB_rec_buf,\
  					   ZB_RECEIVE_DATA_LEN);
  // 用于处理指令的缓存
  // uint8_t ZB_cache[ZB_RECEIVE_DATA_LEN] = {0};
  /* USER CODE END 2 */
 
 

  /* Infinite loop */
  /* USER CODE BEGIN WHILE */
  while (1)
  {
	  // ZigBee接收到指令
	  if(is_ZB_rec == 1){
		  // TODO: 将指令复制到cache

		  is_ZB_rec = 0;
		  // TODO: 解析指令
		  if(ZB_rec_buf[4] == 'a'){
			  HAL_GPIO_TogglePin(GPIOB, GPIO_PIN_13);
		  }else if(ZB_rec_buf[4] == 'b'){
			  setLevLED(0);
		  }else if(ZB_rec_buf[4] == 'c'){
			  setLevLED(1);
		  }else if(ZB_rec_buf[4] == 'd'){
			  setLevLED(2);
		  }
		  // TODO: 处理指令
		  sendToLogByDMA("zb get data! %c, %c, %c, %c, %c\n", ZB_rec_buf[0], ZB_rec_buf[1], ZB_rec_buf[2], ZB_rec_buf[3], ZB_rec_buf[4]);

	  }
    /* USER CODE END WHILE */

    /* USER CODE BEGIN 3 */

	//sendToLogByDMA("v1: %d, v2: %d, v3: %d; v: %d, l: %d\n", voice_t[0], voice_t[1], voice_t[2], voice_current, light_current);
	sendToZBByDMA("abcde");
	HAL_Delay(3000);
  }
  /* USER CODE END 3 */
}

/**
  * @brief System Clock Configuration
  * @retval None
  */
void SystemClock_Config(void)
{
  RCC_OscInitTypeDef RCC_OscInitStruct = {0};
  RCC_ClkInitTypeDef RCC_ClkInitStruct = {0};
  RCC_PeriphCLKInitTypeDef PeriphClkInit = {0};

  /** Initializes the CPU, AHB and APB busses clocks 
  */
  RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSE;
  RCC_OscInitStruct.HSEState = RCC_HSE_ON;
  RCC_OscInitStruct.HSEPredivValue = RCC_HSE_PREDIV_DIV1;
  RCC_OscInitStruct.HSIState = RCC_HSI_ON;
  RCC_OscInitStruct.PLL.PLLState = RCC_PLL_ON;
  RCC_OscInitStruct.PLL.PLLSource = RCC_PLLSOURCE_HSE;
  RCC_OscInitStruct.PLL.PLLMUL = RCC_PLL_MUL7;
  if (HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK)
  {
    Error_Handler();
  }
  /** Initializes the CPU, AHB and APB busses clocks 
  */
  RCC_ClkInitStruct.ClockType = RCC_CLOCKTYPE_HCLK|RCC_CLOCKTYPE_SYSCLK
                              |RCC_CLOCKTYPE_PCLK1|RCC_CLOCKTYPE_PCLK2;
  RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_PLLCLK;
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV2;
  RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV1;

  if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_2) != HAL_OK)
  {
    Error_Handler();
  }
  PeriphClkInit.PeriphClockSelection = RCC_PERIPHCLK_ADC;
  PeriphClkInit.AdcClockSelection = RCC_ADCPCLK2_DIV4;
  if (HAL_RCCEx_PeriphCLKConfig(&PeriphClkInit) != HAL_OK)
  {
    Error_Handler();
  }
}

/**
  * @brief ADC1 Initialization Function
  * @param None
  * @retval None
  */
static void MX_ADC1_Init(void)
{

  /* USER CODE BEGIN ADC1_Init 0 */

  /* USER CODE END ADC1_Init 0 */

  ADC_ChannelConfTypeDef sConfig = {0};

  /* USER CODE BEGIN ADC1_Init 1 */

  /* USER CODE END ADC1_Init 1 */
  /** Common config 
  */
  hadc1.Instance = ADC1;
  hadc1.Init.ScanConvMode = ADC_SCAN_ENABLE;
  hadc1.Init.ContinuousConvMode = DISABLE;
  hadc1.Init.DiscontinuousConvMode = DISABLE;
  hadc1.Init.ExternalTrigConv = ADC_SOFTWARE_START;
  hadc1.Init.DataAlign = ADC_DATAALIGN_RIGHT;
  hadc1.Init.NbrOfConversion = 2;
  if (HAL_ADC_Init(&hadc1) != HAL_OK)
  {
    Error_Handler();
  }
  /** Configure Regular Channel 
  */
  sConfig.Channel = ADC_CHANNEL_1;
  sConfig.Rank = ADC_REGULAR_RANK_1;
  sConfig.SamplingTime = ADC_SAMPLETIME_71CYCLES_5;
  if (HAL_ADC_ConfigChannel(&hadc1, &sConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /** Configure Regular Channel 
  */
  sConfig.Channel = ADC_CHANNEL_2;
  sConfig.Rank = ADC_REGULAR_RANK_2;
  if (HAL_ADC_ConfigChannel(&hadc1, &sConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN ADC1_Init 2 */

  /* USER CODE END ADC1_Init 2 */

}

/**
  * @brief TIM3 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM3_Init(void)
{

  /* USER CODE BEGIN TIM3_Init 0 */

  /* USER CODE END TIM3_Init 0 */

  TIM_ClockConfigTypeDef sClockSourceConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};
  TIM_OC_InitTypeDef sConfigOC = {0};

  /* USER CODE BEGIN TIM3_Init 1 */

  /* USER CODE END TIM3_Init 1 */
  htim3.Instance = TIM3;
  htim3.Init.Prescaler = 28-1;
  htim3.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim3.Init.Period = 100-1;
  htim3.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim3.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  if (HAL_TIM_Base_Init(&htim3) != HAL_OK)
  {
    Error_Handler();
  }
  sClockSourceConfig.ClockSource = TIM_CLOCKSOURCE_INTERNAL;
  if (HAL_TIM_ConfigClockSource(&htim3, &sClockSourceConfig) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_PWM_Init(&htim3) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim3, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sConfigOC.OCMode = TIM_OCMODE_PWM1;
  sConfigOC.Pulse = 0;
  sConfigOC.OCPolarity = TIM_OCPOLARITY_HIGH;
  sConfigOC.OCFastMode = TIM_OCFAST_DISABLE;
  if (HAL_TIM_PWM_ConfigChannel(&htim3, &sConfigOC, TIM_CHANNEL_2) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_PWM_ConfigChannel(&htim3, &sConfigOC, TIM_CHANNEL_3) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_PWM_ConfigChannel(&htim3, &sConfigOC, TIM_CHANNEL_4) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM3_Init 2 */

  /* USER CODE END TIM3_Init 2 */
  HAL_TIM_MspPostInit(&htim3);

}

/**
  * @brief USART1 Initialization Function
  * @param None
  * @retval None
  */
static void MX_USART1_UART_Init(void)
{

  /* USER CODE BEGIN USART1_Init 0 */

  /* USER CODE END USART1_Init 0 */

  /* USER CODE BEGIN USART1_Init 1 */

  /* USER CODE END USART1_Init 1 */
  huart1.Instance = USART1;
  huart1.Init.BaudRate = 115200;
  huart1.Init.WordLength = UART_WORDLENGTH_8B;
  huart1.Init.StopBits = UART_STOPBITS_1;
  huart1.Init.Parity = UART_PARITY_NONE;
  huart1.Init.Mode = UART_MODE_TX_RX;
  huart1.Init.HwFlowCtl = UART_HWCONTROL_NONE;
  huart1.Init.OverSampling = UART_OVERSAMPLING_16;
  if (HAL_UART_Init(&huart1) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN USART1_Init 2 */

  /* USER CODE END USART1_Init 2 */

}

/**
  * @brief USART3 Initialization Function
  * @param None
  * @retval None
  */
static void MX_USART3_UART_Init(void)
{

  /* USER CODE BEGIN USART3_Init 0 */

  /* USER CODE END USART3_Init 0 */

  /* USER CODE BEGIN USART3_Init 1 */

  /* USER CODE END USART3_Init 1 */
  huart3.Instance = USART3;
  huart3.Init.BaudRate = 115200;
  huart3.Init.WordLength = UART_WORDLENGTH_8B;
  huart3.Init.StopBits = UART_STOPBITS_1;
  huart3.Init.Parity = UART_PARITY_NONE;
  huart3.Init.Mode = UART_MODE_TX_RX;
  huart3.Init.HwFlowCtl = UART_HWCONTROL_NONE;
  huart3.Init.OverSampling = UART_OVERSAMPLING_16;
  if (HAL_UART_Init(&huart3) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN USART3_Init 2 */

  /* USER CODE END USART3_Init 2 */

}

/** 
  * Enable DMA controller clock
  */
static void MX_DMA_Init(void) 
{

  /* DMA controller clock enable */
  __HAL_RCC_DMA1_CLK_ENABLE();

  /* DMA interrupt init */
  /* DMA1_Channel1_IRQn interrupt configuration */
  HAL_NVIC_SetPriority(DMA1_Channel1_IRQn, 0, 0);
  HAL_NVIC_EnableIRQ(DMA1_Channel1_IRQn);
  /* DMA1_Channel2_IRQn interrupt configuration */
  HAL_NVIC_SetPriority(DMA1_Channel2_IRQn, 0, 0);
  HAL_NVIC_EnableIRQ(DMA1_Channel2_IRQn);
  /* DMA1_Channel3_IRQn interrupt configuration */
  HAL_NVIC_SetPriority(DMA1_Channel3_IRQn, 0, 0);
  HAL_NVIC_EnableIRQ(DMA1_Channel3_IRQn);
  /* DMA1_Channel4_IRQn interrupt configuration */
  HAL_NVIC_SetPriority(DMA1_Channel4_IRQn, 0, 0);
  HAL_NVIC_EnableIRQ(DMA1_Channel4_IRQn);
  /* DMA1_Channel5_IRQn interrupt configuration */
  HAL_NVIC_SetPriority(DMA1_Channel5_IRQn, 0, 0);
  HAL_NVIC_EnableIRQ(DMA1_Channel5_IRQn);

}

/**
  * @brief GPIO Initialization Function
  * @param None
  * @retval None
  */
static void MX_GPIO_Init(void)
{
  GPIO_InitTypeDef GPIO_InitStruct = {0};

  /* GPIO Ports Clock Enable */
  __HAL_RCC_GPIOD_CLK_ENABLE();
  __HAL_RCC_GPIOA_CLK_ENABLE();
  __HAL_RCC_GPIOB_CLK_ENABLE();

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOB, GPIO_PIN_13, GPIO_PIN_RESET);

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOB, GPIO_PIN_3|GPIO_PIN_4, GPIO_PIN_SET);

  /*Configure GPIO pins : PB13 PB3 PB4 */
  GPIO_InitStruct.Pin = GPIO_PIN_13|GPIO_PIN_3|GPIO_PIN_4;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(GPIOB, &GPIO_InitStruct);

  /*Configure GPIO pin : PB15 */
  GPIO_InitStruct.Pin = GPIO_PIN_15;
  GPIO_InitStruct.Mode = GPIO_MODE_IT_RISING;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  HAL_GPIO_Init(GPIOB, &GPIO_InitStruct);

  /* EXTI interrupt init*/
  HAL_NVIC_SetPriority(EXTI15_10_IRQn, 1, 0);
  HAL_NVIC_EnableIRQ(EXTI15_10_IRQn);

}

/* USER CODE BEGIN 4 */

// 光线传感器的值归一化到0-511
uint8_t sensor_light(uint32_t x){
	return (uint8_t)(x*511/4095);
}

// 声音传感器的值归一化到0-511
uint8_t sensor_voice(uint32_t x){
	return (uint8_t)(x*511/2095);
}

// 返回1/100秒内声音的最大值
uint8_t voice_second_per_max(){
	uint8_t vm = 0;
	for(int i=0; i<VOICE_BUF_PER_SIZE; i++){
		if(voice_s_p[i] > vm){
			vm = voice_s_p[i];
		}
	}
	return vm;
}

// 返回1秒内声音的最大值
uint8_t voice_second_max(){
	uint8_t vm = 0;
	for(int i=0; i<VOICE_BUF_SIZE; i++){
		if(voice_s[i] > vm){
			vm = voice_s[i];
		}
	}
	return vm;
}

// 触摸开关控制灯的亮灭
void HAL_GPIO_EXTI_Callback(uint16_t GPIO_Pin)
{
	if(GPIO_Pin == GPIO_PIN_15){
		HAL_GPIO_TogglePin(GPIOB, GPIO_PIN_13);
	}
}

// TIM3定时器中断,100us一次
void HAL_TIM_PeriodElapsedCallback(TIM_HandleTypeDef *htim){
	  // 执行ADC自动自校准
	  if(HAL_OK != HAL_ADCEx_Calibration_Start(&hadc1))
		  Error_Handler();
	  //开启ADC
	  if(HAL_OK != HAL_ADC_Start_DMA(&hadc1, adc_buf, 2))
		  Error_Handler();
}

// ADC转换完成后回调函数
void HAL_ADC_ConvCpltCallback(ADC_HandleTypeDef* hadc){
	if(HAL_OK != HAL_ADC_Stop_DMA(&hadc1))
		Error_Handler();
	light_current = sensor_light(adc_buf[0]);
	voice_current = sensor_voice(adc_buf[1]);

	// 循环填充声音信号暂存数组
		  if(voice_s_p_idx < VOICE_BUF_PER_SIZE){
			  voice_s_p[voice_s_p_idx] = voice_current;
			  voice_s_p_idx++;
		  }else{
			  // 1/100s
			  voice_s_p_idx = 0;

			  if(voice_s_idx < VOICE_BUF_SIZE){
				  voice_s[voice_s_idx] = voice_second_per_max();
				  voice_s_idx++;
			  }else{
				  // 1s
				  voice_s_idx = 0;

				  if(voice_t_idx < VOICE_STORE_N){
					  voice_t[voice_t_idx] = voice_second_max();
					  // sendToLogByDMA("1s!- %d\n", voice_t[voice_t_idx]);
					  voice_t_idx++;
				  }else{
					  // 3s
					  voice_t_idx = 0;
					  // sendToLogByDMA("3s!- %d - %d - %d\n", voice_t[0], voice_t[1], voice_t[2]);
				  }
			  }
		  }
}

// 串口1收到数据的回调函数
void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart){
	// sendToLogByDMA("usart1 got data!\n");
	// HAL_Delay(20);
	if (huart->Instance == USART1){
		is_ZB_rec = 1;
		// sendToLogByDMA("recive data: %c, %c, %c\n", ZB_rec_buf[0], ZB_rec_buf[1], ZB_rec_buf[2]);
		sendToZBByDMA("abzjb");
	}
}

/* USER CODE END 4 */

/**
  * @brief  This function is executed in case of error occurrence.
  * @retval None
  */
void Error_Handler(void)
{
  /* USER CODE BEGIN Error_Handler_Debug */
  /* User can add his own implementation to report the HAL error return state */

  /* USER CODE END Error_Handler_Debug */
}

#ifdef  USE_FULL_ASSERT
/**
  * @brief  Reports the name of the source file and the source line number
  *         where the assert_param error has occurred.
  * @param  file: pointer to the source file name
  * @param  line: assert_param error line source number
  * @retval None
  */
void assert_failed(uint8_t *file, uint32_t line)
{ 
  /* USER CODE BEGIN 6 */
  /* User can add his own implementation to report the file name and line number,
     tex: printf("Wrong parameters value: file %s on line %d\r\n", file, line) */
  /* USER CODE END 6 */
}
#endif /* USE_FULL_ASSERT */

/************************ (C) COPYRIGHT STMicroelectronics *****END OF FILE****/
