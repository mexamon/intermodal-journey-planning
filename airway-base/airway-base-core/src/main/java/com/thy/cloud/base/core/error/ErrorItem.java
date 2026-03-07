package com.thy.cloud.base.core.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Engin Mahmut
 * Error Items Model
 * @version 1.1
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("unused")
public class ErrorItem
{
    private String code;
    private String message;

}